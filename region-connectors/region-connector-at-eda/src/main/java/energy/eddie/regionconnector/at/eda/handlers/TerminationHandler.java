// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration.PartyIdType;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TerminationHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;
    private final AtConfiguration atConfiguration;
    private final EdaAdapter edaAdapter;
    private final DataNeedsService dataNeedsService;

    public TerminationHandler(
            Outbox outbox,
            EventBus eventBus,
            AtPermissionRequestRepository repository,
            AtConfiguration atConfiguration,
            EdaAdapter edaAdapter,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.atConfiguration = atConfiguration;
        this.edaAdapter = edaAdapter;
        this.dataNeedsService = dataNeedsService;
        eventBus.filteredFlux(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        LOGGER.debug("Terminating permission request {}", permissionId);
        var permissionRequest = repository.getByPermissionId(permissionId);
        var dn = dataNeedsService.getById(permissionRequest.dataNeedId());
        try {
            var revoke = getCcmoRevoke(dn, permissionRequest);
            LOGGER.debug("Sending CCMORevoke for permission request {} from partner ID {}",
                         permissionId,
                         revoke.eligiblePartyId());
            edaAdapter.sendCMRevoke(revoke);
        } catch (Exception e) {
            LOGGER.warn("Error trying to terminate permission request.", e);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FAILED_TO_TERMINATE));
        }
    }

    private CCMORevoke getCcmoRevoke(DataNeed dn, AtPermissionRequest permissionRequest) {
        String epId;
        String reason;
        if (dn instanceof CESUJoinRequestDataNeed) {
            epId = atConfiguration.partyIdFor(PartyIdType.ENERGY_COMMUNITY);
            if (epId == null) {
                throw new IllegalStateException("Energy Community Operator ID is not configured");
            }
            reason = "Terminated by the Energy Community Operator";
        } else {
            epId = atConfiguration.partyIdFor(PartyIdType.ELIGIBLE_PARTY);
            reason = "Terminated by the Eligible Party";
        }
        return new CCMORevoke(permissionRequest, epId, reason);
    }
}

// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class TerminationHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;
    private final AtConfiguration atConfiguration;
    private final EdaGroupingIdFactory groupingIdFactory;
    private final EdaAdapter edaAdapter;

    public TerminationHandler(
            Outbox outbox,
            EventBus eventBus,
            AtPermissionRequestRepository repository,
            AtConfiguration atConfiguration,
            EdaGroupingIdFactory groupingIdFactory,
            EdaAdapter edaAdapter
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.atConfiguration = atConfiguration;
        this.groupingIdFactory = groupingIdFactory;
        this.edaAdapter = edaAdapter;
        eventBus.filteredFlux(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        AtPermissionRequest permissionRequest = request.get();
        try {
            var messageId = groupingIdFactory.create(
                    AtConfiguration.PartyIdType.ELIGIBLE_PARTY,
                    ZonedDateTime.now(EdaRegionConnectorMetadata.AT_ZONE_ID)
            );
            var revoke = new CCMORevoke(
                    permissionRequest,
                    atConfiguration.eligiblePartyId(),
                    messageId,
                    "Terminated by the Eligible Party"
            );
            edaAdapter.sendCMRevoke(revoke);
        } catch (Exception e) {
            LOGGER.warn("Error trying to terminate permission request.", e);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FAILED_TO_TERMINATE));
        }
    }
}

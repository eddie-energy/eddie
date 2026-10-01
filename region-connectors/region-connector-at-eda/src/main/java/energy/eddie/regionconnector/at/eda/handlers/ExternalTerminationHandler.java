// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.TerminationEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class ExternalTerminationHandler implements EventHandler<TerminationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTerminationHandler.class);
    private final Outbox outbox;
    private final AtPermissionRequestRepository repository;
    private final AtConfiguration atConfiguration;
    private final EdaGroupingIdFactory groupingIdFactory;
    private final EdaAdapter edaAdapter;

    public ExternalTerminationHandler(
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
        eventBus.filteredFlux(TerminationEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(TerminationEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        AtPermissionRequest permissionRequest = request.get();
        try {
            var consentEnd = ZonedDateTime.now(AT_ZONE_ID);
            var start = permissionRequest.start().atStartOfDay(AT_ZONE_ID);
            if (consentEnd.isBefore(start)) {
                consentEnd = start;
            }
            var messageId = groupingIdFactory.create(
                    ZonedDateTime.now(AT_ZONE_ID)
            );
            var revoke = new CCMORevoke(
                    permissionRequest,
                    atConfiguration.eligiblePartyId(),
                    messageId,
                    permissionEvent.conversationId(),
                    "Terminated by the Eligible Party",
                    consentEnd
            );
            edaAdapter.sendCMRevoke(revoke);
        } catch (Exception e) {
            LOGGER.warn("Error trying to terminate permission request.", e);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.FAILED_TO_TERMINATE));
        }
    }
}

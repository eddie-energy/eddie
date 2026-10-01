// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.events.TerminationEvent;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class TerminationHandler implements EventHandler<PermissionEvent> {
    private final Outbox outbox;
    private final EdaGroupingIdFactory groupingIdFactory;

    public TerminationHandler(
            Outbox outbox,
            EventBus eventBus,
            EdaGroupingIdFactory groupingIdFactory
    ) {
        this.outbox = outbox;
        this.groupingIdFactory = groupingIdFactory;
        eventBus.filteredFlux(PermissionProcessStatus.TERMINATED)
                .subscribe(this::accept);
    }
    @Override
    public void accept(PermissionEvent permissionEvent) {
        outbox.commit(new TerminationEvent(
                permissionEvent.permissionId(),
                groupingIdFactory.create(ZonedDateTime.now(AT_ZONE_ID))
        ));
    }
}

// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UnfulfillableHandler implements EventHandler<PermissionEvent> {
    private final Outbox outbox;

    public UnfulfillableHandler(Outbox outbox, EventBus eventBus) {
        this.outbox = outbox;
        eventBus.filteredFlux(PermissionProcessStatus.UNFULFILLABLE)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent event) {
        outbox.commit(new SimpleEvent(event.permissionId(), PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
    }
}

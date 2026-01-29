// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import org.springframework.stereotype.Component;

@Component
public class UnfulfillableHandler implements EventHandler<UsUnfulfillableEvent> {
    private final Outbox outbox;

    public UnfulfillableHandler(Outbox outbox, EventBus eventBus) {
        this.outbox = outbox;
        eventBus.filteredFlux(UsUnfulfillableEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(UsUnfulfillableEvent event) {
        if (event.requiresExternalTermination()) {
            outbox.commit(new UsSimpleEvent(event.permissionId(),
                                            PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
        }
    }
}

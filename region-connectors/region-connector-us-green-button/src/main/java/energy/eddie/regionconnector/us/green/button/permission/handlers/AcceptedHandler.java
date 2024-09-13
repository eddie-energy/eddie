package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsPollingNotReadyEvent;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {

    private final PollingService pollingService;

    public AcceptedHandler(PollingService pollingService, EventBus eventBus) {
        this.pollingService = pollingService;
        eventBus.filteredFlux(PermissionProcessStatus.ACCEPTED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent event) {
        if (event instanceof UsPollingNotReadyEvent || event instanceof UsMeterReadingUpdateEvent) {
            return;
        }
        pollingService.poll(event.permissionId());
    }
}

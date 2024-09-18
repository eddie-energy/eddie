package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.springframework.stereotype.Component;

@Component
public class StartPollingEventHandler implements EventHandler<UsStartPollingEvent> {

    private final PollingService pollingService;

    public StartPollingEventHandler(
            PollingService pollingService, EventBus eventBus
    ) {
        this.pollingService = pollingService;
        eventBus.filteredFlux(UsStartPollingEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(UsStartPollingEvent event) {
        pollingService.poll(event.permissionId());
    }
}

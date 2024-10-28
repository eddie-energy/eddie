package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StartPollingEventHandler implements EventHandler<UsStartPollingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartPollingEventHandler.class);
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
        var permissionId = event.permissionId();
        LOGGER.info("Starting to poll data for permission request {}", permissionId);
        pollingService.poll(permissionId);
    }
}

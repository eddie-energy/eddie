package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.regionconnector.be.fluvius.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.service.polling.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcceptedEventHandler implements EventHandler<AcceptedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedEventHandler.class);
    private final PollingService pollingService;
    private final BePermissionRequestRepository repository;

    public AcceptedEventHandler(
            EventBus eventBus,
            PollingService pollingService,
            BePermissionRequestRepository repository
    ) {
        this.repository = repository;
        eventBus.filteredFlux(AcceptedEvent.class).subscribe(this::accept);
        this.pollingService = pollingService;
    }

    @Override
    public void accept(AcceptedEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        LOGGER.info("Received accepted event for permission {}", permissionId);
        var pr = repository.getByPermissionId(permissionId);
        if (pollingService.isActiveAndNeedsToBeFetched(pr)) {
            pollingService.pollTimeSeriesData(pr);
        } else {
            LOGGER.info("Data for permission request {} not yet available", permissionId);
        }
    }
}

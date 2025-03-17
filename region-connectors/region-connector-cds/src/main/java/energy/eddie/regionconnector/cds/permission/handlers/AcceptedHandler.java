package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.regionconnector.cds.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<AcceptedEvent> {
    private final CdsPermissionRequestRepository repository;
    private final PollingService pollingService;

    public AcceptedHandler(
            EventBus eventBus,
            CdsPermissionRequestRepository repository,
            PollingService pollingService
    ) {
        this.repository = repository;
        this.pollingService = pollingService;
        eventBus.filteredFlux(AcceptedEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(AcceptedEvent event) {
        var pr = repository.getByPermissionId(event.permissionId());
        pollingService.poll(pr);
    }
}

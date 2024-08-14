package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class AcceptedHandler implements EventHandler<PermissionEvent> {

    private final PollingService pollingService;
    private final FiPermissionRequestRepository repository;

    public AcceptedHandler(PollingService pollingService, FiPermissionRequestRepository repository, EventBus eventBus) {
        this.pollingService = pollingService;
        this.repository = repository;
        eventBus.filteredFlux(PermissionProcessStatus.ACCEPTED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionRequest = repository.getByPermissionId(permissionEvent.permissionId());
        pollingService.pollTimeSeriesData(permissionRequest);
    }
}

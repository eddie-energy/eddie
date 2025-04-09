package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.services.TerminationService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class RequiresExternalTerminationHandler implements EventHandler<PermissionEvent> {
    private final CdsPermissionRequestRepository repository;
    private final TerminationService terminationService;

    public RequiresExternalTerminationHandler(
            CdsPermissionRequestRepository repository,
            TerminationService terminationService,
            EventBus eventBus
    ) {
        this.repository = repository;
        this.terminationService = terminationService;
        eventBus.filteredFlux(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionRequest = repository.getByPermissionId(permissionEvent.permissionId());
        terminationService.terminate(permissionRequest);
    }
}

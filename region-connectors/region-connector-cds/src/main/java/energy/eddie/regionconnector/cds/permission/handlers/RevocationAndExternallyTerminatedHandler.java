package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.persistence.OAuthCredentialsRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RevocationAndExternallyTerminatedHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevocationAndExternallyTerminatedHandler.class);
    private final OAuthCredentialsRepository repository;

    public RevocationAndExternallyTerminatedHandler(OAuthCredentialsRepository repository, EventBus eventBus) {
        this.repository = repository;
        eventBus.filteredFlux(PermissionProcessStatus.EXTERNALLY_TERMINATED)
                .subscribe(this::accept);
        eventBus.filteredFlux(PermissionProcessStatus.REVOKED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent event) {
        var permissionId = event.permissionId();
        LOGGER.info("permission request {} was {}, removing credentials", permissionId, event.status());
        repository.deleteByPermissionId(permissionId);
    }
}

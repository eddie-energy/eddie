package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class RequiresExternalTerminationHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequiresExternalTerminationHandler.class);
    private final GreenButtonApi api;
    private final UsPermissionRequestRepository repository;
    private final Outbox outbox;

    public RequiresExternalTerminationHandler(
            EventBus eventBus,
            GreenButtonApi api,
            UsPermissionRequestRepository repository,
            Outbox outbox
    ) {
        eventBus.filteredFlux(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
                .subscribe(this::accept);
        this.api = api;
        this.repository = repository;
        this.outbox = outbox;
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var pr = repository.getByPermissionId(permissionId);
        api.revoke(pr.authorizationUid(), pr.dataSourceInformation().meteredDataAdministratorId())
           .subscribe(res -> outbox.commit(new UsSimpleEvent(permissionId,
                                                             PermissionProcessStatus.EXTERNALLY_TERMINATED)),
                      err -> handleError(err, permissionId));
    }

    private void handleError(Throwable err, String permissionId) {
        switch (err) {
            case WebClientResponseException.NotFound notFound -> {
                LOGGER.warn(
                        "Permission request {} could not be found at permission administrator",
                        permissionId,
                        notFound
                );
                outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.EXTERNALLY_TERMINATED));
            }
            default -> {
                LOGGER.info("Permission request {} could not be externally terminated, retrying later",
                            permissionId);
                outbox.commit(new UsSimpleEvent(permissionId,
                                                PermissionProcessStatus.FAILED_TO_TERMINATE));
            }
        }
    }
}

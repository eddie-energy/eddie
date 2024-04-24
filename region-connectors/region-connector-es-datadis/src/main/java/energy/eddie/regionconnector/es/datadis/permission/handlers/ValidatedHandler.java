package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSentToPermissionAdministratorEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsValidatedEvent;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidatedHandler implements EventHandler<EsValidatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHandler.class);
    private final Outbox outbox;
    private final AuthorizationApi authorizationApi;
    private final AuthorizationRequestFactory authorizationRequestFactory;
    private final EsPermissionRequestRepository repository;

    public ValidatedHandler(
            Outbox outbox,
            EventBus eventBus,
            AuthorizationApi authorizationApi,
            AuthorizationRequestFactory authorizationRequestFactory,
            EsPermissionRequestRepository repository
    ) {
        this.outbox = outbox;
        this.authorizationApi = authorizationApi;
        this.authorizationRequestFactory = authorizationRequestFactory;
        this.repository = repository;
        eventBus.filteredFlux(EsValidatedEvent.class)
                .subscribe(this::accept);
    }


    @Override
    public void accept(EsValidatedEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        LOGGER.info("Sending permission request {} to Datadis", permissionId);
        var pr = repository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.error("Got unknown permission request {}", permissionId);
            return;
        }
        var permissionRequest = pr.get();
        var authorizationRequest = authorizationRequestFactory.from(
                permissionRequest.nif(),
                permissionRequest.meteringPointId(),
                permissionEvent.end()
        );
        authorizationApi.postAuthorizationRequest(authorizationRequest)
                        .doOnError(e -> {
                            LOGGER.warn("Could not send permission request {} to Datadis", permissionId);
                            outbox.commit(new EsSimpleEvent(permissionId,
                                                            PermissionProcessStatus.UNABLE_TO_SEND));
                        })
                        .subscribe(res -> outbox.commit(new EsSentToPermissionAdministratorEvent(permissionId,
                                                                                                 res.originalResponse())));
    }
}

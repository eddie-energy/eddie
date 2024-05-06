package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.events.EsInvalidEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSentToPermissionAdministratorEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SentHandler implements EventHandler<EsSentToPermissionAdministratorEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SentHandler.class);
    private final Outbox outbox;

    public SentHandler(Outbox outbox, EventBus eventBus) {
        this.outbox = outbox;
        eventBus.filteredFlux(EsSentToPermissionAdministratorEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(EsSentToPermissionAdministratorEvent permissionEvent) {
        var permissionId = permissionEvent.permissionId();
        var response = permissionEvent.response();
        switch (response) {
            case AuthorizationRequestResponse.NoNif ignored ->
                    outbox.commit(new EsInvalidEvent(permissionId, "Given NIF does not exist"));
            case AuthorizationRequestResponse.NoPermission ignored ->
                    outbox.commit(new EsInvalidEvent(permissionId, "The given NIF has no permissions"));
            case AuthorizationRequestResponse.Unknown ignored -> outbox.commit(new EsInvalidEvent(permissionId,
                                                                                                  "Unknown response from datadis: " + response.originalResponse()));
            case AuthorizationRequestResponse.Ok ignored ->
                    LOGGER.info("Permission request {} was successfully sent to Datadis", permissionId);
        }
    }
}

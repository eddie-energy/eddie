package energy.eddie.regionconnector.at.eda.handlers.integration.outbound;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class ConnectionStatusMessageHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusMessageHandler.class);
    private final Sinks.Many<ConnectionStatusMessage> messages;
    private final AtPermissionRequestRepository repository;

    public ConnectionStatusMessageHandler(EventBus eventBus,
                                          Sinks.Many<ConnectionStatusMessage> messages,
                                          AtPermissionRequestRepository repository) {
        this.messages = messages;
        this.repository = repository;
        eventBus.filteredFlux(PermissionEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        String permissionId = permissionEvent.permissionId();
        var optionalRequest = repository.findByPermissionId(permissionId);
        if (optionalRequest.isEmpty()) {
            LOGGER.warn("Got event without permission request for permission id {}", permissionId);
            return;
        }
        var permissionRequest = optionalRequest.get();
        messages.tryEmitNext(
                new ConnectionStatusMessage(
                        permissionRequest.connectionId(),
                        permissionRequest.permissionId(),
                        permissionRequest.dataNeedId(),
                        permissionRequest.dataSourceInformation(),
                        permissionEvent.status(),
                        permissionRequest.message()
                )
        );
    }
}

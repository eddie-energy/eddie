package energy.eddie.regionconnector.shared.event.sourcing.handlers.integration;

import com.fasterxml.jackson.databind.JsonNode;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Function;

/**
 * Subscribes to all events of an {@code EventBus} and creates connection status messages based on an event.
 */
public class ConnectionStatusMessageHandler<T extends PermissionRequest> implements EventHandler<PermissionEvent>, ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusMessageHandler.class);
    private final Sinks.Many<ConnectionStatusMessage> messages = Sinks.many().multicast().onBackpressureBuffer();
    private final PermissionRequestRepository<T> repository;
    private final Function<T, String> messageFunc;
    private final Function<T, JsonNode> additionalDataFunc;

    public ConnectionStatusMessageHandler(
            EventBus eventBus,
            PermissionRequestRepository<T> repository,
            Function<T, String> messageFunc
    ) {
        this(eventBus, repository, messageFunc, permissionRequest -> null);
    }

    public ConnectionStatusMessageHandler(
            EventBus eventBus,
            PermissionRequestRepository<T> repository,
            Function<T, String> messageFunc,
            Function<T, JsonNode> additionalDataFunc
    ) {
        this.repository = repository;
        this.messageFunc = messageFunc;
        this.additionalDataFunc = additionalDataFunc;
        eventBus.filteredFlux(PermissionEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        if (permissionEvent instanceof InternalPermissionEvent) {
            return;
        }
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
                        messageFunc.apply(permissionRequest),
                        additionalDataFunc.apply(permissionRequest)
                )
        );
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return messages.asFlux();
    }

    @Override
    public void close() {
        messages.tryEmitComplete();
    }
}

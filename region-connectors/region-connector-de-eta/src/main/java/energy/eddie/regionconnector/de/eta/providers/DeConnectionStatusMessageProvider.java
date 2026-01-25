package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import reactor.core.publisher.Flux;

/**
 * Provides connection status messages for the German (DE) region connector.
 * This class streams connection status updates to outbound connectors.
 * 
 * Note: This is NOT a @Component - it's registered as a bean in the Spring config
 * to avoid ambiguity with the ConnectionStatusMessageHandler bean.
 */
public class DeConnectionStatusMessageProvider implements ConnectionStatusMessageProvider {
    
    private final ConnectionStatusMessageHandler<DePermissionRequest> handler;

    public DeConnectionStatusMessageProvider(ConnectionStatusMessageHandler<DePermissionRequest> handler) {
        this.handler = handler;
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return handler.getConnectionStatusMessageStream();
    }

    @Override
    public void close() throws Exception {
        handler.close();
    }
}

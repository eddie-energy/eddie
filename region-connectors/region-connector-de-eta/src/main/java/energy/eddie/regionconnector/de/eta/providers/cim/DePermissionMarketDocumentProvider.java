package energy.eddie.regionconnector.de.eta.providers.cim;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Provides CIM v0.82 Permission Market Documents for the German (DE) region connector.
 * This class streams permission status updates in CIM format to outbound connectors.
 * 
 * Note: This is registered as a bean via the Spring config, not as a @Component.
 */
public class DePermissionMarketDocumentProvider implements PermissionMarketDocumentProvider {
    
    private final PermissionMarketDocumentMessageHandler<DePermissionRequest> handler;

    public DePermissionMarketDocumentProvider(PermissionMarketDocumentMessageHandler<DePermissionRequest> handler) {
        this.handler = handler;
    }

    @Override
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return handler.getPermissionMarketDocumentStream();
    }

    @Override
    public void close() throws Exception {
        handler.close();
    }
}

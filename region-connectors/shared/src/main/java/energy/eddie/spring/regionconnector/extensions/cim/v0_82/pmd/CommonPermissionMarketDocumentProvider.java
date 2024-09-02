package energy.eddie.spring.regionconnector.extensions.cim.v0_82.pmd;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class CommonPermissionMarketDocumentProvider implements PermissionMarketDocumentProvider {
    private final Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink;

    public CommonPermissionMarketDocumentProvider(Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink) {
        this.permissionMarketDocumentSink = permissionMarketDocumentSink;
    }

    @Override
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return permissionMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        permissionMarketDocumentSink.tryEmitComplete();
    }
}

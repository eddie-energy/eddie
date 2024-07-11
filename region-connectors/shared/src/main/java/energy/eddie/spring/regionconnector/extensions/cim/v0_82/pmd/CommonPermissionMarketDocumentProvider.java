package energy.eddie.spring.regionconnector.extensions.cim.v0_82.pmd;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class CommonPermissionMarketDocumentProvider implements PermissionMarketDocumentProvider {
    private final Sinks.Many<PermissionEnveloppe> permissionMarketDocumentSink;

    public CommonPermissionMarketDocumentProvider(Sinks.Many<PermissionEnveloppe> permissionMarketDocumentSink) {
        this.permissionMarketDocumentSink = permissionMarketDocumentSink;
    }

    @Override
    public Flux<PermissionEnveloppe> getPermissionMarketDocumentStream() {
        return permissionMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        permissionMarketDocumentSink.tryEmitComplete();
    }
}

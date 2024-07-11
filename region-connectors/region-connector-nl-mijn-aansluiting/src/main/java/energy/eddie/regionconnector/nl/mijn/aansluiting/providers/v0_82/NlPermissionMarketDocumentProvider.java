package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class NlPermissionMarketDocumentProvider implements PermissionMarketDocumentProvider {
    private final Sinks.Many<PermissionEnveloppe> permissionMarketDocumentSink;

    public NlPermissionMarketDocumentProvider(Sinks.Many<PermissionEnveloppe> permissionMarketDocumentSink) {this.permissionMarketDocumentSink = permissionMarketDocumentSink;}

    @Override
    public Flux<PermissionEnveloppe> getPermissionMarketDocumentStream() {
        return permissionMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        permissionMarketDocumentSink.tryEmitComplete();
    }
}

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class NlPermissionMarketDocumentProvider implements PermissionMarketDocumentProvider {
    private final Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink;

    public NlPermissionMarketDocumentProvider(Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink) {this.permissionMarketDocumentSink = permissionMarketDocumentSink;}

    @Override
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return permissionMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        permissionMarketDocumentSink.tryEmitComplete();
    }
}

package energy.eddie.core.services;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PermissionMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionMarketDocumentService.class);
    private final Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink = Sinks.many()
                                                                                      .multicast()
                                                                                      .onBackpressureBuffer();

    public void registerProvider(PermissionMarketDocumentProvider statusMessageProvider) {
        LOGGER.info("PermissionService: Registering {}", statusMessageProvider.getClass().getName());
        statusMessageProvider.getPermissionMarketDocumentStream()
                             .doOnNext(permissionMarketDocumentSink::tryEmitNext)
                             .doOnError(permissionMarketDocumentSink::tryEmitError)
                             .subscribe();
    }

    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return permissionMarketDocumentSink.asFlux();
    }
}

package energy.eddie.core.services;

import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.ConsentMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ConsentMarketDocumentService implements ConsentMarketDocumentServiceInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMarketDocumentService.class);
    private final Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink = Sinks.many()
                                                                                     .multicast()
                                                                                     .onBackpressureBuffer();

    public void registerProvider(ConsentMarketDocumentProvider statusMessageProvider) {
        LOGGER.info("PermissionService: Registering {}", statusMessageProvider.getClass().getName());
        statusMessageProvider.getConsentMarketDocumentStream()
                             .doOnNext(consentMarketDocumentSink::tryEmitNext)
                             .doOnError(consentMarketDocumentSink::tryEmitError)
                             .subscribe();
    }

    public Flux<ConsentMarketDocument> getConsentMarketDocumentStream() {
        return consentMarketDocumentSink.asFlux();
    }
}

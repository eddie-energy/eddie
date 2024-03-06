package energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd;

import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class CommonConsentMarketDocumentProvider implements ConsentMarketDocumentProvider {
    private final Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink;

    public CommonConsentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink) {
        this.consentMarketDocumentSink = consentMarketDocumentSink;
    }

    @Override
    public Flux<ConsentMarketDocument> getConsentMarketDocumentStream() {
        return consentMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        consentMarketDocumentSink.tryEmitComplete();
    }
}

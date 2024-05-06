package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class NlConsentMarketDocumentProvider implements energy.eddie.api.v0_82.ConsentMarketDocumentProvider {
    private final Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink;

    public NlConsentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink) {this.consentMarketDocumentSink = consentMarketDocumentSink;}

    @Override
    public Flux<ConsentMarketDocument> getConsentMarketDocumentStream() {
        return consentMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        consentMarketDocumentSink.tryEmitComplete();
    }
}

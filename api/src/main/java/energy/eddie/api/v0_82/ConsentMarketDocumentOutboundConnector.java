package energy.eddie.api.v0_82;

import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import reactor.core.publisher.Flux;

public interface ConsentMarketDocumentOutboundConnector {
    void setConsentMarketDocumentStream(Flux<ConsentMarketDocument> consentMarketDocumentStream);
}

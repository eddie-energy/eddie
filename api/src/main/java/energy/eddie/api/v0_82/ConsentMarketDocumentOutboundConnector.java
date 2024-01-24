package energy.eddie.api.v0_82;

import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;

import java.util.concurrent.Flow;

public interface ConsentMarketDocumentOutboundConnector {
    void setConsentMarketDocumentStream(Flow.Publisher<ConsentMarketDocument> consentMarketDocumentStream);
}

package energy.eddie.api.v0_82;

import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;

import java.util.concurrent.Flow;

public interface ConsentMarketDocumentProvider extends AutoCloseable {
    /**
     * Data stream of all ConsentMarketDocument updates created by this region connector.
     * The ConsentMarketDocument will contain the new state of the Consent in the process
     *
     * @return ConsentMarketDocument stream that can be consumed only once
     */
    Flow.Publisher<ConsentMarketDocument> getConsentMarketDocumentStream();
}

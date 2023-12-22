package energy.eddie.api.v0_82;

import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;

import java.util.concurrent.Flow;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
public interface EddieValidatedHistoricalDataMarketDocumentOutboundConnector {
    /**
     * Sets the stream of validated historical data market documents to be sent to the EP app.
     *
     * @param marketDocumentStream stream of validated historical data market documents
     */
    void setEddieValidatedHistoricalDataMarketDocumentStream(Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> marketDocumentStream);
}
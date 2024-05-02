package energy.eddie.api.v0_82;

import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors are passed on to
 * the EP using their transport of choice, e.g. Kafka.
 */
public interface EddieAccountingPointMarketDocumentOutboundConnector {
    /**
     * Sets the stream of validated historical data market documents to be sent to the EP app.
     *
     * @param marketDocumentStream stream of validated historical data market documents
     */
    void setEddieAccountingPointMarketDocumentStream(
            Flux<EddieAccountingPointMarketDocument> marketDocumentStream
    );
}

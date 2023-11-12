package energy.eddie.api.v0_82;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;

import java.util.concurrent.Flow;

/**
 * An application connector delivers data to the eligible party's application. All messages from the region connectors
 * are passed on to the EP application using their transport of choice, e.g. Kafka or RDBMS access.
 */
public interface ApplicationConnector {
    /**
     * Sets the stream of connection status messages to be sent to the EP app.
     *
     * @param connectionStatusMessageStream stream of connection status messages
     */
    void setConnectionStatusMessageStream(Flow.Publisher<ConnectionStatusMessage> connectionStatusMessageStream);

    /**
     * Sets the stream of validated historical data market documents to be sent to the EP app.
     *
     * @param marketDocumentStream stream of validated historical data market documents
     */
    void setEddieValidatedHistoricalDataMarketDocumentStream(Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> marketDocumentStream);
}
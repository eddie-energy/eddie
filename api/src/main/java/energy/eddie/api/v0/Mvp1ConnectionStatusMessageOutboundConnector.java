package energy.eddie.api.v0;

import java.util.concurrent.Flow;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
public interface Mvp1ConnectionStatusMessageOutboundConnector {
    /**
     * Sets the stream of connection status messages to be sent to the EP app.
     *
     * @param connectionStatusMessageStream stream of connection status messages
     */
    void setConnectionStatusMessageStream(Flow.Publisher<ConnectionStatusMessage> connectionStatusMessageStream);
}

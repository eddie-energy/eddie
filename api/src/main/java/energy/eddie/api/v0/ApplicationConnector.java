package energy.eddie.api.v0;

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
     * Sets the stream of consumption records to be sent to the EP app.
     *
     * @param consumptionRecordStream stream of consumption records
     */
    void setConsumptionRecordStream(Flow.Publisher<ConsumptionRecord> consumptionRecordStream);

    /**
     * Initialize and start application connector (life-cycle method).
     * @deprecated since it introduces temporal coupling
     */
    @Deprecated(since = "it introduces temporal coupling")
    void init();
}

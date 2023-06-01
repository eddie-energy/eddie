package energy.eddie.api.v0;

import java.util.concurrent.Flow;

/**
 * An application connector delivers data to the eligible party's application. All messages from the region connectors
 * are passed on to the EP application using their transport of choice, e.g. Kafka or RDBMS access.
 *
 * <hr><b>TODO</b> This interface should be refined and re-documented as the timing can be quite diverse (e.g. should
 * the set methods be called before or after init() ? What happends if events are sent before init is called?
 * Do we also neeed a stop method?)
 */
public interface ApplicationConnector {
    /**
     * Sets the stream of connection status messages to be sent to the EP app.
     * @param csmsFlow stream of connection status messages
     */
    void setConnectionStatusMessageStream(Flow.Publisher<ConnectionStatusMessage> csmsFlow);

    /**
     * Sets the stream of consumption records to be sent to the EP app.
     * @param crsFlow stream of consumption records
     */
    void setConsumptionRecordStream(Flow.Publisher<ConsumptionRecord> crsFlow);

    /**
     * Initialize and start application connector (life-cycle method).
     */
    void init();
}

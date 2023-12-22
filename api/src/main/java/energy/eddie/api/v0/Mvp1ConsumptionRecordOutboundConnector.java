package energy.eddie.api.v0;

import java.util.concurrent.Flow;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
public interface Mvp1ConsumptionRecordOutboundConnector {
    /**
     * Sets the stream of consumption records to be sent to the EP app.
     *
     * @param consumptionRecordStream stream of consumption records
     */
    void setConsumptionRecordStream(Flow.Publisher<ConsumptionRecord> consumptionRecordStream);
}

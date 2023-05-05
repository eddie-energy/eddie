package eddie.energy.regionconnector.api.v0;


import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;

import java.util.concurrent.Flow;

public interface ConsumptionRecordPublisher {
    void subscribeToConsumptionRecordPublisher(Flow.Subscriber<ConsumptionRecord> subscriber);
}
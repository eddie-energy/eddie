package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.util.concurrent.Flow;

@Component
public class EnedisMvp1ConsumptionRecordProvider implements energy.eddie.api.v0.Mvp1ConsumptionRecordProvider {

    private final Flux<IdentifiableMeterReading> meterReadings;

    public EnedisMvp1ConsumptionRecordProvider(Flux<IdentifiableMeterReading> meterReadings) {
        this.meterReadings = meterReadings;
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                meterReadings
                        .map(IntermediateConsumptionRecord::new)
                        .map(IntermediateConsumptionRecord::consumptionRecord)
        );
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
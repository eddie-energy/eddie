package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnedisMvp1ConsumptionRecordProvider implements energy.eddie.api.v0.Mvp1ConsumptionRecordProvider {

    private final Flux<IdentifiableMeterReading> meterReadings;

    public EnedisMvp1ConsumptionRecordProvider(Flux<IdentifiableMeterReading> meterReadings) {
        this.meterReadings = meterReadings;
    }

    @Override
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return meterReadings
                .map(IntermediateConsumptionRecord::new)
                .map(IntermediateConsumptionRecord::consumptionRecord);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
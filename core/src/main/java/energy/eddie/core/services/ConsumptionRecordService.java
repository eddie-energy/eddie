package energy.eddie.core.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordService.class);
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();

    public void registerProvider(Mvp1ConsumptionRecordProvider consumptionRecordProvider) {
        LOGGER.info("ConsumptionRecordService: Registering {}", consumptionRecordProvider.getClass().getName());
        consumptionRecordProvider.getConsumptionRecordStream()
                .doOnNext(consumptionRecordSink::tryEmitNext)
                .doOnError(consumptionRecordSink::tryEmitError)
                .subscribe();
    }

    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordSink.asFlux();
    }
}

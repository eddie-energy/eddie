package energy.eddie.core.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.concurrent.Flow;

@Service
public class ConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordService.class);
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();

    public void registerProvider(Mvp1ConsumptionRecordProvider consumptionRecordProvider) {
        LOGGER.info("ConsumptionRecordService: Registering {}", consumptionRecordProvider.getClass().getName());
        JdkFlowAdapter.flowPublisherToFlux(consumptionRecordProvider.getConsumptionRecordStream())
                .doOnNext(consumptionRecordSink::tryEmitNext)
                .doOnError(consumptionRecordSink::tryEmitError)
                .subscribe();
    }

    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordSink.asFlux());
    }
}

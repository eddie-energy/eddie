package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.concurrent.Flow;

@RestController
public class ConsumptionRecordController implements Mvp1ConsumptionRecordProvider, AutoCloseable {
    private final Sinks.Many<ConsumptionRecord> consumptionRecordStreamSink = Sinks.many().multicast().onBackpressureBuffer();

    @PostMapping("/api/consumption-records")
    public void produceConsumptionRecord(@RequestBody ConsumptionRecord consumptionRecord) {
        consumptionRecordStreamSink.tryEmitNext(consumptionRecord);
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordStreamSink.asFlux());
    }

    @Override
    public void close() {
        consumptionRecordStreamSink.tryEmitComplete();
    }
}

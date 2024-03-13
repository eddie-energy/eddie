package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.simulation.permission.request.IntermediateValidatedHistoricalDataMarketDocument;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
public class ConsumptionRecordController implements Mvp1ConsumptionRecordProvider, EddieValidatedHistoricalDataMarketDocumentProvider, AutoCloseable {

    private final Sinks.Many<ConsumptionRecord> consumptionRecordStreamSink = Sinks.many().multicast()
            .onBackpressureBuffer();
    private final Sinks.Many<EddieValidatedHistoricalDataMarketDocument> vhdmdSink = Sinks.many().multicast()
            .onBackpressureBuffer();

    private final CommonInformationModelConfiguration cimConfig;

    public ConsumptionRecordController(CommonInformationModelConfiguration cimConfig) {
        this.cimConfig = cimConfig;
    }

    @PostMapping("/api/consumption-records")
    public void produceConsumptionRecord(@RequestBody ConsumptionRecord consumptionRecord) {
        consumptionRecordStreamSink.tryEmitNext(consumptionRecord);
        vhdmdSink.tryEmitNext(
                new IntermediateValidatedHistoricalDataMarketDocument(consumptionRecord, cimConfig).value());
    }


    @Override
    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordStreamSink.asFlux();
    }

    @Override
    public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return vhdmdSink.asFlux();
    }

    @Override
    public void close() {
        consumptionRecordStreamSink.tryEmitComplete();
        vhdmdSink.tryEmitComplete();
    }
}

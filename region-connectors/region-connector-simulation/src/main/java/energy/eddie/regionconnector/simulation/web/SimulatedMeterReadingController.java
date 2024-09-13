package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.permission.request.IntermediateValidatedHistoricalDataMarketDocument;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
public class SimulatedMeterReadingController implements ValidatedHistoricalDataEnvelopeProvider, AutoCloseable {

    private final Sinks.Many<ValidatedHistoricalDataEnvelope> vhdmdSink = Sinks.many().multicast()
                                                                               .onBackpressureBuffer();

    private final CommonInformationModelConfiguration cimConfig;

    public SimulatedMeterReadingController(CommonInformationModelConfiguration cimConfig) {
        this.cimConfig = cimConfig;
    }

    @PostMapping("/simulated-meter-reading")
    public void produceMeterReading(@RequestBody SimulatedMeterReading simulatedMeterReading) {
        vhdmdSink.tryEmitNext(
                new IntermediateValidatedHistoricalDataMarketDocument(simulatedMeterReading, cimConfig).value());
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return vhdmdSink.asFlux();
    }

    @Override
    public void close() {
        vhdmdSink.tryEmitComplete();
    }
}

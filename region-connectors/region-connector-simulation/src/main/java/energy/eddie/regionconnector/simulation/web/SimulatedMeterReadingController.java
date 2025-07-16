package energy.eddie.regionconnector.simulation.web;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.permission.request.IntermediateValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulatedMeterReadingController {
    private final DocumentStreams streams;
    private final CommonInformationModelConfiguration cimConfig;

    public SimulatedMeterReadingController(DocumentStreams streams, CommonInformationModelConfiguration cimConfig) {
        this.streams = streams;
        this.cimConfig = cimConfig;
    }

    @PostMapping("/simulated-meter-reading")
    public void produceMeterReading(@RequestBody SimulatedMeterReading simulatedMeterReading) {
        streams.publish(
                new IntermediateValidatedHistoricalDataMarketDocument(simulatedMeterReading, cimConfig).value()
        );
    }
}

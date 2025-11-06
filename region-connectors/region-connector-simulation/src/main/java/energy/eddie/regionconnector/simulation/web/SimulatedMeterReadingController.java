package energy.eddie.regionconnector.simulation.web;

import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulatedMeterReadingController {
    private final DocumentStreams streams;

    public SimulatedMeterReadingController(DocumentStreams streams) {
        this.streams = streams;
    }

    @PostMapping("/simulated-meter-reading")
    public void produceMeterReading(@RequestBody SimulatedMeterReading simulatedMeterReading) {
        streams.publish(simulatedMeterReading);
    }
}

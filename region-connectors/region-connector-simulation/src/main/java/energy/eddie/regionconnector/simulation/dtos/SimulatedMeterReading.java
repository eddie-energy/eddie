package energy.eddie.regionconnector.simulation.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

public record SimulatedMeterReading(
        String connectionId,
        String dataNeedId,
        String permissionId,
        String meteringPoint,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        @JsonProperty("startDateTime")
        ZonedDateTime startDateTime,
        String meteringInterval,
        List<Measurement> measurements
) {
}

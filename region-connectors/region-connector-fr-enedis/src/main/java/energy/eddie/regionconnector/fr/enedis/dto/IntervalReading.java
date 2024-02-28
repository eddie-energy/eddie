package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record IntervalReading(
        @JsonProperty("value")
        String value,
        @JsonProperty("date")
        String date,
        @JsonProperty("measure_type")
        Optional<String> measureType,
        @JsonProperty("interval_length")
        Optional<String> intervalLength
) {
}
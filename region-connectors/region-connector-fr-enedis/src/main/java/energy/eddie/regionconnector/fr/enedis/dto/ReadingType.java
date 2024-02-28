package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record ReadingType(
        @JsonProperty("unit")
        String unit,
        @JsonProperty("measurement_kind")
        String measurementKind,
        @JsonProperty("aggregate")
        String aggregate,
        @JsonProperty("measuring_period")
        Optional<String> measuringPeriod
) {
}
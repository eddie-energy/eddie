package energy.eddie.regionconnector.simulation.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Measurement(
        @JsonProperty("value")
        Double value,
        @JsonProperty("measurementType")
        MeasurementType measurementType
) {

    public enum MeasurementType {
        MEASURED,
        EXTRAPOLATED;

        @JsonCreator
        public static MeasurementType fromValue(String value) {
            return switch (value) {
                case "measured" -> MEASURED;
                case "extrapolated" -> EXTRAPOLATED;
                default -> throw new IllegalArgumentException(value);
            };
        }
    }
}

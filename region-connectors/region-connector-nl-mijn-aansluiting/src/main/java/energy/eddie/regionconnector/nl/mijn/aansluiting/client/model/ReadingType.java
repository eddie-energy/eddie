package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadingType(
        @JsonProperty("Multiplier")
        MultiplierEnum multiplier,
        @JsonProperty("Unit")
        UnitEnum unit
) {
    public enum MultiplierEnum {
        @JsonProperty("k")
        K
    }

    public enum UnitEnum {
        @JsonProperty("m3")
        M3,
        @JsonProperty("Wh")
        WH
    }
}
package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnedisDataApiResponse(
        @JsonProperty("meter_reading")
        MeterReading meterReading
) {
}
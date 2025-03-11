package energy.eddie.aiida.datasources.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.record.UnitOfMeasurement;

/**
 * Represents the JSON that is received from the inbound data source.
 *
 * @param timestamp Timestamp of the data.
 * @param code      OBIS-Code of the data.
 * @param value     Value of the data.
 * @param unit      Unit of the data.
 */
public record InboundDataSourceJson(
        @JsonProperty("timestamp")
        Long timestamp,
        @JsonProperty("code")
        String code,
        @JsonProperty("value")
        Double value,
        @JsonProperty("unit")
        UnitOfMeasurement unit
) { }

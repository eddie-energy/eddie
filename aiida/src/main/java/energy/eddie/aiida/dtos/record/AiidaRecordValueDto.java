package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiidaRecordValueDto(
        @JsonProperty String obisCode,
        @JsonProperty ObisCode rawTag,
        @JsonProperty String rawValue,
        @JsonProperty UnitOfMeasurement rawUnit,
        @JsonProperty String value,
        @JsonProperty UnitOfMeasurement unit,
        @JsonProperty String sourceKey
) {}
package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiidaRecordValueDto(
        @JsonProperty String rawTag,
        @JsonProperty ObisCode dataTag,
        @JsonProperty String rawValue,
        @JsonProperty String value,
        @JsonProperty UnitOfMeasurement rawUnitOfMeasurement,
        @JsonProperty UnitOfMeasurement unitOfMeasurement
) {
}

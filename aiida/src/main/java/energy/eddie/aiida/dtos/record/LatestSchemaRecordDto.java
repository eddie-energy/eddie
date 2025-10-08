package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;

import java.time.Instant;

public record LatestSchemaRecordDto(
        @JsonProperty AiidaSchema schema,
        @JsonProperty Instant sentAt,
        @JsonProperty String message
) {
}

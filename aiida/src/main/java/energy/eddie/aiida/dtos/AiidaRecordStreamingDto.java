package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Contains the data of an {@link energy.eddie.aiida.models.record.AiidaRecord} and additional metadata that should
 * be sent along with each record to the EP.
 */
public record AiidaRecordStreamingDto(
        @JsonProperty Instant timestamp,
        @JsonProperty String code,
        @JsonProperty Object value,
        @JsonProperty String connectionId,
        @JsonProperty String dataNeedId,
        @JsonProperty String permissionId
) {
}

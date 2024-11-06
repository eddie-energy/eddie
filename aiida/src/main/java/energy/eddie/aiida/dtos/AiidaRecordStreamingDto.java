package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.record.AiidaRecordValue;

import java.time.Instant;
import java.util.List;

/**
 * Contains the data of an {@link energy.eddie.aiida.models.record.AiidaRecord} and additional metadata that should
 * be sent along with each record to the EP.
 */
public record AiidaRecordStreamingDto(
        @JsonProperty Instant timestamp,
        @JsonProperty String asset,
        @JsonProperty String connectionId,
        @JsonProperty String dataNeedId,
        @JsonProperty String permissionId,
        @JsonProperty("values")
        List<AiidaRecordValue> aiidaRecordValues
) {
}

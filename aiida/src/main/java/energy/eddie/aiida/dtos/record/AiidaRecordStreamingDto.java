package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contains the data of an {@link energy.eddie.aiida.models.record.AiidaRecord} and additional metadata that should
 * be sent along with each record to the EP.
 */
public record AiidaRecordStreamingDto(
        @JsonProperty Instant timestamp,
        @JsonProperty AiidaAsset asset,
        @JsonProperty String connectionId,
        @JsonProperty UUID dataNeedId,
        @JsonProperty UUID permissionId,
        @JsonProperty UUID dataSourceId,
        @JsonProperty("values")
        List<AiidaRecordValue> aiidaRecordValues
) {
}

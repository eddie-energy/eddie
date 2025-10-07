package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;

import java.time.Instant;

public record LatestInboundPermissionRecordDto(
        @JsonProperty Instant timestamp,
        @JsonProperty AiidaAsset asset,
        @JsonProperty String payload
) {
}
// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record InboundRecordDto(
        @JsonProperty Instant timestamp,
        @JsonProperty UUID userId,
        @JsonProperty UUID dataSourceId,
        @JsonProperty AiidaAsset asset,
        @JsonProperty @Nullable String meterId,
        @JsonProperty @Nullable String operatorId,
        @JsonProperty String payload
) {
}

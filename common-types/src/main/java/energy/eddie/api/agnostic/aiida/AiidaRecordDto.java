// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiidaRecordDto(
        @JsonProperty Instant timestamp,
        @JsonProperty UUID permissionId,
        @JsonProperty UUID userId,
        @JsonProperty UUID dataSourceId,
        @JsonProperty AiidaAsset asset,
        @JsonProperty @Nullable String meterId,
        @JsonProperty @Nullable String operatorId,
        @JsonProperty(value = "values") List<AiidaRecordValueDto> aiidaRecordValues
) {
}

// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record AiidaRecordDto(
        @JsonProperty AiidaAsset asset,
        @JsonProperty UUID userId,
        @JsonProperty UUID dataSourceId,
        @JsonProperty UUID permissionId,
        @JsonProperty(value = "values") List<AiidaRecordValueDto> aiidaRecordValues
) {
}

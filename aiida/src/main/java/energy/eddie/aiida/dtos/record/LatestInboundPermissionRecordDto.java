// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaAsset;

import java.time.Instant;

public record LatestInboundPermissionRecordDto(
        @JsonProperty Instant timestamp,
        @JsonProperty AiidaAsset asset,
        @JsonProperty String payload
) {
}
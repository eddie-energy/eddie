// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaSchema;

import java.time.Instant;

public record LatestSchemaRecordDto(
        @JsonProperty AiidaSchema schema,
        @JsonProperty Instant sentAt,
        @JsonProperty String message
) {
}

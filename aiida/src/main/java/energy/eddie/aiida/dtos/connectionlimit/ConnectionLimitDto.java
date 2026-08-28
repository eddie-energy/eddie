// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.connectionlimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConnectionLimitDto(
        @JsonProperty
        @Schema(description = "Permission ID this limit belongs to.", example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
        UUID permissionId,
        @JsonProperty
        @Schema(description = "Meter ID this limit applies to, if provided.", example = "003114735")
        String meterId,
        @JsonProperty
        @Schema(description = "Market document ID that produced this limit. Null if the default limit was used.", example = "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12")
        @Nullable
        String documentId,
        @JsonProperty
        @Schema(description = "Start time of the interval (inclusive) in UTC format.", example = "2026-07-10T08:45:00Z")
        Instant intervalStart,
        @JsonProperty
        @Schema(description = "End time of the interval (exclusive) in UTC format.", example = "2026-07-10T09:00:00Z")
        Instant intervalEnd,
        @JsonProperty
        @Schema(description = "Minimum allowed connection limit in kW.", example = "3.0")
        BigDecimal minLimitKw,
        @JsonProperty
        @Schema(description = "Maximum allowed connection limit in kW.", example = "8.0")
        BigDecimal maxLimitKw
) {
}

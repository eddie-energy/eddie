// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record MeasurementPointDto(
        @JsonProperty
        @Schema(description = "Timestamp of the measurement in UTC format.", example = "2026-07-10T08:45:00Z")
        Instant timestamp,
        @JsonProperty
        @Schema(description = "Minimum signed power in kW within the point's interval.", example = "3.9")
        BigDecimal minPowerKw,
        @JsonProperty
        @Schema(description = "Maximum signed power in kW within the point's interval.", example = "4.2")
        BigDecimal maxPowerKw
) {
}

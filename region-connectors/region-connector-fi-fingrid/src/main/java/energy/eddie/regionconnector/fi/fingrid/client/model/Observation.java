// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record Observation(
        @JsonProperty("PeriodStartTime") ZonedDateTime start,
        @JsonProperty("Quantity") BigDecimal quantity,
        @JsonProperty("Quality") String quality
) {}

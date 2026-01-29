// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Reading(
        @JsonProperty("DateAndOrTime")
        DateAndOrTime dateAndOrTime,
        @JsonProperty("ReadingType")
        ReadingType readingType,
        @JsonProperty("Value")
        BigDecimal value
) {}


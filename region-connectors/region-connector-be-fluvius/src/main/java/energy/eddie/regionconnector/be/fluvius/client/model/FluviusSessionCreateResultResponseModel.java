// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.OffsetDateTime;

public record FluviusSessionCreateResultResponseModel(
        @JsonProperty("status") @Nullable String status,
        @JsonProperty("shortUrlIdentifier") @Nullable String shortUrlIdentifier,
        @JsonProperty("validTo") @Nullable OffsetDateTime validTo
) {}


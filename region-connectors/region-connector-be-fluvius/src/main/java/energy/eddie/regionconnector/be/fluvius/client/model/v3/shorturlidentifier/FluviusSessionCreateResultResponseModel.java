// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.shorturlidentifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public record FluviusSessionCreateResultResponseModel(
        @JsonProperty("status") Status status,
        @JsonProperty("shortUrlIdentifier") @Nullable String shortUrlIdentifier,
        @JsonProperty("validTo") @Nullable ZonedDateTime validTo
) {
    public enum Status {
        @JsonProperty("success")
        SUCCESS,
        @JsonProperty("failed")
        FAILED
    }
}


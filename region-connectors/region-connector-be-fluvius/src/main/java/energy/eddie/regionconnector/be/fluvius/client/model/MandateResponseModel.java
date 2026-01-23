// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public record MandateResponseModel(
        @JsonProperty("referenceNumber") @Nullable String referenceNumber,
        @JsonProperty("status") @Nullable String status,
        @JsonProperty("eanNumber") @Nullable String eanNumber,
        @JsonProperty("energyType") @Nullable String energyType,
        @JsonProperty("dataPeriodFrom") @Nullable ZonedDateTime dataPeriodFrom,
        @JsonProperty("dataPeriodTo") @Nullable ZonedDateTime dataPeriodTo,
        @JsonProperty("dataServiceType") @Nullable String dataServiceType,
        @JsonProperty("mandateExpirationDate") @Nullable ZonedDateTime mandateExpirationDate,
        @JsonProperty("renewalStatus") @Nullable String renewalStatus
) {}
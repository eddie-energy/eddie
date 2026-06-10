// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.shorturlidentifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record FluviusSessionRequestDataServiceModel(
        @JsonProperty("dataServiceType") @Nullable String dataServiceType,
        @JsonProperty("dataPeriodFrom") @Nullable String dataPeriodFrom,
        @JsonProperty("dataPeriodTo") @Nullable String dataPeriodTo
) {}


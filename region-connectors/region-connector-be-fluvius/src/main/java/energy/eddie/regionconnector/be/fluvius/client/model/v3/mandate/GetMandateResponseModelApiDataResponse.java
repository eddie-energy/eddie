// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.ApiMetaData;
import org.jspecify.annotations.Nullable;

public record GetMandateResponseModelApiDataResponse(
        @JsonProperty("_meta") @Nullable ApiMetaData metaData,
        @JsonProperty("data") @Nullable GetMandateResponseModel data
) {}
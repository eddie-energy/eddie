// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.be.fluvius.client.model.ApiMetaData;
import jakarta.annotation.Nullable;

public record CreateMandateResponseModelApiDataResponse(
        @Nullable @JsonProperty("metaData") ApiMetaData metaData,
        @Nullable @JsonProperty("data") CreateMandateResponseModel data
) {}


// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record FluviusSessionCreateResultResponseModelApiDataResponse(
        @JsonProperty("metaData") @Nullable ApiMetaData metaData,
        @JsonProperty("data") @Nullable FluviusSessionCreateResultResponseModel data
) {}


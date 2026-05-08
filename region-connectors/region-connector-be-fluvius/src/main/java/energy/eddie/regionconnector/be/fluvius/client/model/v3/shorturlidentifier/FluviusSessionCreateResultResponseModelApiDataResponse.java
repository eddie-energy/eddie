// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.shorturlidentifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.ApiMetaData;
import org.jspecify.annotations.Nullable;

public record FluviusSessionCreateResultResponseModelApiDataResponse(@JsonProperty("_meta") @Nullable ApiMetaData meta,
                                                                     @Nullable FluviusSessionCreateResultResponseModel data) {
}

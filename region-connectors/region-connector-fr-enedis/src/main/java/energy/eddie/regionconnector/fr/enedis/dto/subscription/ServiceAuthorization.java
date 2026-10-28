// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Authorization details of a subscribed service.
 *
 * @param id    Identifier of the authorization.
 * @param label Label of the authorization.
 * @param type  Type of the authorization (explicit, induced or implicit).
 */
public record ServiceAuthorization(
        @JsonProperty("autorisationId") @Nullable Long id,
        @JsonProperty("autorisationLibelle") @Nullable String label,
        @JsonProperty("autorisationType") @Nullable AuthorizationType type
) {
}

// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Contact coordinates of the customer.
 */
public record ContactData(
        @JsonProperty("email") @Nullable String email,
        @JsonProperty("landline") @Nullable String landline,
        @JsonProperty("phone") @Nullable String phone
) {
}

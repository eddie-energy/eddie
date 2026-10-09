// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Organization customer.
 */
public record Organization(
        @JsonProperty("name") @Nullable String name,
        @JsonProperty("commercial_name") @Nullable String commercialName,
        @JsonProperty("business_code") @Nullable String businessCode,
        @JsonProperty("siret_number") @Nullable String siretNumber,
        @JsonProperty("siren_number") @Nullable String sirenNumber
) {
}

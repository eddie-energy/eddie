// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Natural person customer.
 */
public record Person(
        @JsonProperty("title") @Nullable String title,
        @JsonProperty("lastname") @Nullable String lastName,
        @JsonProperty("firstname") @Nullable String firstName
) {
}

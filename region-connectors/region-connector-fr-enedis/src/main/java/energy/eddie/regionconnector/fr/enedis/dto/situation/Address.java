// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Address of the customer (not the PRM address).
 */
public record Address(
        @JsonProperty("line1") @Nullable String line1,
        @JsonProperty("line2") @Nullable String line2,
        @JsonProperty("line3") @Nullable String line3,
        @JsonProperty("line4") @Nullable String line4,
        @JsonProperty("line5") @Nullable String line5,
        @JsonProperty("line6") @Nullable String line6,
        @JsonProperty("line7") @Nullable String line7
) {
}

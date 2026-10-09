// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Address holder nested under the customer block. The JSON key is spelled {@code adress} in the
 * Enedis response (their spelling, not a typo on our side).
 */
public record CustomerAddress(
        @JsonProperty("adress") @Nullable Address adress
) {
}
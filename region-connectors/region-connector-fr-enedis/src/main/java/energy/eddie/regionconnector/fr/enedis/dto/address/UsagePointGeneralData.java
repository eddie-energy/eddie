// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Response of the données générales API ({@code GET /donnees_generales_auto/v1/{usage_point_id}}).
 */
public record UsagePointGeneralData(
        @JsonProperty("address") @Nullable AddressData address
) {
}

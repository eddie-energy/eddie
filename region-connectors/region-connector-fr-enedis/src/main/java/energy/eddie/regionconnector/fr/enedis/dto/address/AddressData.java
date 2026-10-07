// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper around the installation address as returned by the données générales API.
 */
public record AddressData(
        @JsonProperty("address") @Nullable InstallationAddress address
) {
}

// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Postal address of the installation (PRM).
 */
public record InstallationAddress(
        @JsonProperty("staircase_floor_apartment") @Nullable String staircaseFloorApartment,
        @JsonProperty("building") @Nullable String building,
        @JsonProperty("number_street_name") @Nullable String numberStreetName,
        @JsonProperty("locality") @Nullable String locality,
        @JsonProperty("postal_code_city") @Nullable String postalCodeCity,
        @JsonProperty("insee_code") @Nullable String inseeCode
) {
}

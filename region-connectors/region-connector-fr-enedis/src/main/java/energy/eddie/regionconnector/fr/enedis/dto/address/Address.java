// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Address(
        @JsonProperty("street")
        String street,
        @JsonProperty("locality")
        String locality,
        @JsonProperty("postal_code")
        String postalCode,
        @JsonProperty("insee_code")
        String inseeCode,
        @JsonProperty("city")
        String city,
        @JsonProperty("country")
        String country,
        @JsonProperty("geo_points")
        GeoPoint geoPoints
) {
}

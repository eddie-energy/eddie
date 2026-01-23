// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeoPoint(
        @JsonProperty("latitude")
        String latitude,
        @JsonProperty("longitude")
        String longitude,
        @JsonProperty("altitude")
        String altitude
) {
}

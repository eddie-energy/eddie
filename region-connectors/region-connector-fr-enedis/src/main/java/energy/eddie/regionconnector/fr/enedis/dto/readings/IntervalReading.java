// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.readings;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record IntervalReading(
        @JsonProperty("value")
        String value,
        @JsonProperty("date")
        String date,
        @JsonProperty("measure_type")
        Optional<String> measureType,
        @JsonProperty("interval_length")
        Optional<String> intervalLength
) {
}

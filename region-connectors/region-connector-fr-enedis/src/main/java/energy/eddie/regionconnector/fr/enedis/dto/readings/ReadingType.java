// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.readings;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record ReadingType(
        @JsonProperty("unit")
        String unit,
        @JsonProperty("measurement_kind")
        String measurementKind,
        @JsonProperty("aggregate")
        String aggregate,
        @JsonProperty("measuring_period")
        Optional<String> measuringPeriod
) {
}

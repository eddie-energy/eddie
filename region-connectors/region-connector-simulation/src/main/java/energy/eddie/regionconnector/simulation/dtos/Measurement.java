// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

public record Measurement(
        @JsonProperty("value")
        Double value,
        @JsonProperty("measurementType")
        MeasurementType measurementType
) {

    public enum MeasurementType {
        MEASURED,
        EXTRAPOLATED;

        @JsonCreator
        public static MeasurementType fromValue(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "measured" -> MEASURED;
                case "extrapolated" -> EXTRAPOLATED;
                default -> throw new IllegalArgumentException(value);
            };
        }
    }
}

// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum SourceCategory {
    INVERTER,
    BATTERY,
    ELECTRICITY_METER_AC,
    ELECTRICITY_METER_DC,
    PV,
    CHARGING_STATION_AC,
    CHARGING_STATION_DC,
    UNKNOWN;

    @JsonCreator
    public static SourceCategory fromString(String value) {
        try {
            return SourceCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}


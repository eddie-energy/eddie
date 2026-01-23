// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceCategoryTest {

    @Test
    @DisplayName("Should return correct enum for valid input strings (case-insensitive)")
    void testFromStringValidInputs() {
        assertEquals(SourceCategory.INVERTER, SourceCategory.fromString("inverter"));
        assertEquals(SourceCategory.BATTERY, SourceCategory.fromString("BATTERY"));
        assertEquals(SourceCategory.ELECTRICITY_METER_AC, SourceCategory.fromString("Electricity_Meter_AC"));
        assertEquals(SourceCategory.ELECTRICITY_METER_DC, SourceCategory.fromString("electricity_meter_dc"));
        assertEquals(SourceCategory.PV, SourceCategory.fromString("Pv"));
        assertEquals(SourceCategory.CHARGING_STATION_AC, SourceCategory.fromString("charging_station_ac"));
        assertEquals(SourceCategory.CHARGING_STATION_DC, SourceCategory.fromString("CHARGING_STATION_DC"));
        assertEquals(SourceCategory.UNKNOWN, SourceCategory.fromString("UNKNOWN")); // Explicitly test passing "UNKNOWN"
    }

    @Test
    @DisplayName("Should return UNKNOWN for invalid or null input")
    void testFromStringInvalidOrUnknownInputs() {
        assertEquals(SourceCategory.UNKNOWN, SourceCategory.fromString("nonexistent_category"));
        assertEquals(SourceCategory.UNKNOWN, SourceCategory.fromString("")); // Empty string
        assertEquals(SourceCategory.UNKNOWN, SourceCategory.fromString("123")); // Numeric string
        assertEquals(SourceCategory.UNKNOWN, SourceCategory.fromString(null)); // Null input
    }
}

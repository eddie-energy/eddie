// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasurementScaleTest {
    @Test
    void testScaled_scalesValue() {
        // Given
        var measurementScale = new MeasurementScale<>(UnitOfMeasureTypeList.AMPERE, 3);

        // When
        var res = measurementScale.scaled(2);

        // Then
        assertEquals(2000, res.intValueExact());
    }
}
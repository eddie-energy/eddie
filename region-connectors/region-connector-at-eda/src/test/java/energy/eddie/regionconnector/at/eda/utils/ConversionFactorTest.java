// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversionFactorTest {

    private static final double KILO = 1000;
    private static final double MEGA = 1000000;
    private static final double GIGA = 1000000000;

    @Test
    void getFactor_ReturnsExpectedValues() {
        assertEquals(KILO, ConversionFactor.KWH_TO_WH.getFactor());
        assertEquals(MEGA, ConversionFactor.MWH_TO_WH.getFactor());
        assertEquals(GIGA, ConversionFactor.GWH_TO_WH.getFactor());
    }

}
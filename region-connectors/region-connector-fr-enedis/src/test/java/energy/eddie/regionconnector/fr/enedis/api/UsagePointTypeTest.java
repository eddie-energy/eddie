// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsagePointTypeTest {

    @ParameterizedTest
    @CsvSource(value = {
            "true, false, CONSUMPTION",
            "false, true, PRODUCTION",
            "true, true, CONSUMPTION_AND_PRODUCTION",
            "false, false, null"
    }, nullValues = "null")
    void fromBooleans(boolean consumption, boolean production, UsagePointType expected) {
        // When
        var result = UsagePointType.fromBooleans(consumption, production).orElse(null);

        // Then
        assertEquals(expected, result);
    }
}

// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

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

    @Test
    void fromSegments_withConsumptionSegment_returnsConsumption() {
        assertEquals(Optional.of(UsagePointType.CONSUMPTION), UsagePointType.fromSegments(List.of("C5")));
    }

    @Test
    void fromSegments_withProductionSegment_returnsProduction() {
        assertEquals(Optional.of(UsagePointType.PRODUCTION), UsagePointType.fromSegments(List.of("P4")));
    }

    @Test
    void fromSegments_withBothSegments_returnsConsumptionAndProduction() {
        assertEquals(Optional.of(UsagePointType.CONSUMPTION_AND_PRODUCTION),
                     UsagePointType.fromSegments(List.of("C4", "P5")));
    }

    @Test
    void fromSegments_withEmptyOrUnknownSegments_returnsEmpty() {
        assertEquals(Optional.empty(), UsagePointType.fromSegments(List.of()));
        assertEquals(Optional.empty(), UsagePointType.fromSegments(List.of("XYZ")));
    }
}

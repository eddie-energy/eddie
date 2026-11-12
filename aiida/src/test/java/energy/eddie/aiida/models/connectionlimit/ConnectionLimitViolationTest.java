// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionLimitViolationTest {
    private static final Instant START = Instant.parse("2026-07-10T10:00:00Z");

    @Test
    void givenStartPower_bothExtremesAreTheStartPower() {
        var violation = violation("9");

        assertEquals(0, new BigDecimal("9").compareTo(violation.lowestPowerKw()));
        assertEquals(0, new BigDecimal("9").compareTo(violation.highestPowerKw()));
    }

    @Test
    void registerPower_higherValue_updatesHighestOnly() {
        var violation = violation("9");

        assertTrue(violation.registerPower(new BigDecimal("10")));
        assertFalse(violation.registerPower(new BigDecimal("9.5")));

        assertEquals(0, BigDecimal.TEN.compareTo(violation.highestPowerKw()));
        assertEquals(0, new BigDecimal("9").compareTo(violation.lowestPowerKw()));
        assertEquals(0, new BigDecimal("9").compareTo(violation.startPowerKw()));
    }

    @Test
    void registerPower_lowerValue_updatesLowestOnly() {
        var violation = violation("1");

        assertTrue(violation.registerPower(new BigDecimal("-2")));
        assertFalse(violation.registerPower(new BigDecimal("0")));

        assertEquals(0, new BigDecimal("-2").compareTo(violation.lowestPowerKw()));
        assertEquals(0, new BigDecimal("1").compareTo(violation.highestPowerKw()));
    }

    @Test
    void registerPower_alternatingDirections_tracksBothExtremes() {
        var violation = violation("9");

        assertTrue(violation.registerPower(new BigDecimal("12")));
        assertTrue(violation.registerPower(new BigDecimal("-3")));
        assertFalse(violation.registerPower(new BigDecimal("11")));

        assertEquals(0, new BigDecimal("12").compareTo(violation.highestPowerKw()));
        assertEquals(0, new BigDecimal("-3").compareTo(violation.lowestPowerKw()));
        assertEquals(0, new BigDecimal("9").compareTo(violation.startPowerKw()));
    }

    @Test
    void end_truncatesToMicroseconds() {
        var violation = violation("9");

        violation.end(Instant.parse("2026-07-10T10:05:00.123456789Z"));

        assertEquals(Instant.parse("2026-07-10T10:05:00.123456Z"), violation.endedAt());
    }

    private ConnectionLimitViolation violation(String startPowerKw) {
        return new ConnectionLimitViolation(UUID.randomUUID(),
                                            UUID.randomUUID(),
                                            START,
                                            null,
                                            new BigDecimal("3"),
                                            new BigDecimal("8"),
                                            new BigDecimal(startPowerKw));
    }
}

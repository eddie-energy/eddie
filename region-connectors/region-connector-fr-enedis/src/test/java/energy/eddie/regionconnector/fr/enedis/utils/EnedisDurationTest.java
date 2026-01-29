// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisDurationTest {

    @Test
    void testEnedisDuration_returnsISODurationOfOneDay_ifEndIsInPast() {
        // Given
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        EnedisDuration duration = new EnedisDuration(end);

        // When
        var res = duration.toString();

        // Then
        assertEquals("P1D", res);
    }

    @ParameterizedTest
    @MethodSource("testEnedisDuration_returnsISODuration_ifEndIsInFuture_methodSource")
    void testEnedisDuration_returnsISODuration_ifEndIsInFuture(LocalDate start, LocalDate end, String expected) {
        // Given
        var startInstant = Instant.from(start.atStartOfDay(ZoneOffset.UTC));
        EnedisDuration duration = new EnedisDuration(end, Clock.fixed(startInstant, ZoneOffset.UTC));

        // When
        var res = duration.toString();

        // Then
        assertEquals(expected, res);
    }

    private static Stream<Arguments> testEnedisDuration_returnsISODuration_ifEndIsInFuture_methodSource() {
        LocalDate now = LocalDate.of(2021, 1, 1);
        return Stream.of(
                Arguments.of(now, now.plusDays(3), "P3D"),
                Arguments.of(now, now.plusYears(3), "P1095D")
        );
    }
}

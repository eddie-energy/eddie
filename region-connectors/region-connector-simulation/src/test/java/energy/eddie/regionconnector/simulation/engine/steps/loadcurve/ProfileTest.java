// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.loadcurve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileTest {
    @ParameterizedTest
    @MethodSource("interpolationValues")
    void testInterpolation(double hours, double expected) {
        // Given
        var values = List.of(BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.valueOf(100),
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO,
                             BigDecimal.ZERO);
        var profile = new Profile(values);

        // When
        var res = profile.get(Duration.ofMinutes((long) (hours * 60)), BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.valueOf(expected).setScale(res.scale(), RoundingMode.UNNECESSARY), res);
    }

    @Test
    void testLoopingIndex() {
        // Given
        var values = Stream.iterate(BigDecimal.ZERO, v -> v.add(BigDecimal.ONE))
                           .limit(24)
                           .toList();
        var profile = new Profile(values);

        // When
        var res = profile.get(Duration.ofHours(32), BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.valueOf(8).setScale(res.scale(), RoundingMode.UNNECESSARY), res);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30})
    void testScaling(int minutes) {
        // Given
        var values = List.of(
                new BigDecimal("0.8")
        );
        var profile = new Profile(values);

        // When
        var res = profile.get(Duration.ofMinutes(minutes), BigDecimal.ONE);

        // Then
        assertEquals(BigDecimal.valueOf(0.8).setScale(res.scale(), RoundingMode.UNNECESSARY), res);
    }

    private static Stream<Arguments> interpolationValues() {
        return Stream.of(
                Arguments.of(12.0, 100.0),
                Arguments.of(12.5, 50.0),
                Arguments.of(11.5, 50.0),
                Arguments.of(11.25, 25.0),
                Arguments.of(11.75, 75.0),
                Arguments.of(12.25, 75.0),
                Arguments.of(12.75, 25.0),
                Arguments.of(12.9, 10.0)
        );
    }
}
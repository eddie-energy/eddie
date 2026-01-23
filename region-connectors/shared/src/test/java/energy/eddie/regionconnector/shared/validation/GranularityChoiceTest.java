// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.validation;

import energy.eddie.api.agnostic.Granularity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GranularityChoiceTest {

    @Test
    void testFind_withValidGranularity() {
        // Given
        var choice = new GranularityChoice(List.of(Granularity.PT1H, Granularity.PT30M, Granularity.P1D));

        // When
        var res = choice.find(Granularity.PT5M, Granularity.P1Y);

        // Then
        assertEquals(Granularity.PT30M, res);
    }

    @Test
    void testFind_withTooSmallGranularity_returnsNull() {
        // Given
        var choice = new GranularityChoice(List.of(Granularity.P1D));

        // When
        var res = choice.find(Granularity.PT5M, Granularity.PT1H);

        // Then
        assertNull(res);
    }

    @Test
    void testFind_withTooLargeGranularity_returnsNull() {
        // Given
        var choice = new GranularityChoice(List.of(Granularity.P1D));

        // When
        var res = choice.find(Granularity.P1Y, Granularity.P1Y);

        // Then
        assertNull(res);
    }

    @Test
    void testFindAll_returnsAllMatchingGranularities() {
        // Given
        var choice = new GranularityChoice(List.of(Granularity.PT30M,
                                                   Granularity.PT1H,
                                                   Granularity.P1D,
                                                   Granularity.P1Y));

        // When
        var res = choice.findAll(Granularity.PT1H, Granularity.P1M);

        // Then
        assertEquals(List.of(Granularity.PT1H, Granularity.P1D), res);
    }
}
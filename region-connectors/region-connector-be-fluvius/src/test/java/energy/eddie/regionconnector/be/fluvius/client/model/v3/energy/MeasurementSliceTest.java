// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.jspecify.annotations.Nullable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MeasurementSliceTest {
    private final ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("measurementsWithoutUnit")
    void firstUnit_withoutReading_returnsNull(@Nullable List<Measurement> measurements) {
        var slice = new MeasurementSlice(start, start.plusHours(1), measurements);

        assertNull(slice.firstUnit(Reading.ReadingKind.TOTAL, Measurement::offtake));
    }

    @Test
    void firstUnit_usesSelectedDirection() {
        var offtake = new Reading(new Total(1.0, Unit.KWH, null, null));
        var injection = new Reading(new Total(2.0, Unit.KVA, null, null));
        var measurement = new Measurement(offtake, injection, null, null);
        var slice = new MeasurementSlice(start, start.plusHours(1), List.of(measurement));

        assertEquals(Unit.KWH, slice.firstUnit(Reading.ReadingKind.TOTAL, Measurement::offtake));
        assertEquals(Unit.KVA, slice.firstUnit(Reading.ReadingKind.TOTAL, Measurement::injection));
    }

    private static Stream<List<Measurement>> measurementsWithoutUnit() {
        return Stream.of(Collections.singletonList(null),
                         List.of(new Measurement(null, null, null, null)),
                         List.of(new Measurement(new Reading(null, null, null, null, null, null), null, null, null)));
    }
}

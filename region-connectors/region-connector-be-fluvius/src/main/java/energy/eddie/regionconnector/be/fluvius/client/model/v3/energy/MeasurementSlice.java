// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

public record MeasurementSlice(ZonedDateTime start, ZonedDateTime end, @Nullable List<Measurement> measurements) {
    public @Nullable Unit firstUnit(
            Reading.ReadingKind kind,
            Function<Measurement, @Nullable Reading> readingSelector
    ) {
        if (measurements == null || measurements.isEmpty()) {
            return null;
        }
        var measurement = measurements.getFirst();
        if (measurement == null) {
            return null;
        }
        var reading = readingSelector.apply(measurement);
        var total = reading == null ? null : reading.value(kind);
        return total == null ? null : total.unit();
    }
}

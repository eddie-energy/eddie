// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import energy.eddie.api.agnostic.Granularity;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public interface MeasurementDevice {
    @Nullable
    List<MeasurementSlice> dailyEnergy();

    @Nullable
    List<MeasurementSlice> hourlyEnergy();

    @Nullable
    List<MeasurementSlice> quarterHourlyEnergy();

    @Nullable
    default List<MeasurementSlice> getForGranularity(Granularity granularity) {
        return switch (granularity) {
            case P1D -> dailyEnergy();
            case PT1H -> hourlyEnergy();
            case PT15M -> quarterHourlyEnergy();
            default -> null;
        };
    }

    @Nullable
    default ZonedDateTime getEarliestReadingForGranularity(Granularity granularity) {
        return findExtremeReadings(granularity, ZonedDateTime::isBefore, MeasurementSlice::start);
    }

    @Nullable
    default ZonedDateTime getLatestReadingForGranularity(Granularity granularity) {
        return findExtremeReadings(granularity, ZonedDateTime::isAfter, MeasurementSlice::end);
    }

    default boolean dataPresentFor(Granularity granularity) {
        return getForGranularity(granularity) != null;
    }

    @Nullable
    private ZonedDateTime findExtremeReadings(
            Granularity granularity,
            BiPredicate<ZonedDateTime, ZonedDateTime> comparator,
            Function<MeasurementSlice, ZonedDateTime> readingFunction
    ) {
        var readings = getForGranularity(granularity);
        if (readings == null) {
            return null;
        }
        ZonedDateTime extremeReading = null;
        for (var reading : readings) {
            var current = readingFunction.apply(reading);
            if (extremeReading == null || (current != null && comparator.test(current, extremeReading))) {
                extremeReading = current;
            }
        }
        return extremeReading;
    }
}

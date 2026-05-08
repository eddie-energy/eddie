// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import energy.eddie.api.agnostic.Granularity;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public interface EnergyForGranularity {
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
    default ZonedDateTime getLatestReadingForGranularity(Granularity granularity) {
        var readings = getForGranularity(granularity);
        if (readings == null) {
            return null;
        }
        ZonedDateTime latestReading = null;
        for (var reading : readings) {
            var current = reading.end();
            if (latestReading == null || current != null && current.isAfter(latestReading)) {
                latestReading = current;
            }
        }
        return latestReading;
    }

    default boolean dataPresentFor(Granularity granularity) {
        return getForGranularity(granularity) != null;
    }
}

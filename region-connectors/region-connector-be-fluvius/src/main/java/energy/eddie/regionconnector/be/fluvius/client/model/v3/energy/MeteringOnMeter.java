// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import energy.eddie.api.agnostic.Granularity;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public class MeteringOnMeter extends Headpoint {
    public static final String DISCRIMINATOR_VALUE = "metering-on-meter";
    @Nullable
    private final List<PhysicalMeter> physicalMeters;

    public MeteringOnMeter(
            @Nullable String ean,
            EnergyType energyType,
            @Nullable List<PhysicalMeter> physicalMeters
    ) {
        super(DISCRIMINATOR_VALUE, ean, energyType);
        this.physicalMeters = physicalMeters;
    }

    public @Nullable List<PhysicalMeter> physicalMeters() {
        return physicalMeters;
    }

    public boolean dataPresentFor(Granularity granularity) {
        return physicalMeters != null
               && physicalMeters.stream().anyMatch(sub -> sub.dataPresentFor(granularity));
    }

    @Override
    public @Nullable ZonedDateTime getLatestMeterReading(Granularity granularity) {
        if (physicalMeters == null) {
            return null;
        }
        ZonedDateTime latest = null;
        for (var physicalMeter : physicalMeters) {
            var current = physicalMeter.getLatestReadingForGranularity(granularity);
            if (latest == null || current != null && current.isAfter(latest)) {
                latest = current;
            }
        }
        return latest;
    }
}

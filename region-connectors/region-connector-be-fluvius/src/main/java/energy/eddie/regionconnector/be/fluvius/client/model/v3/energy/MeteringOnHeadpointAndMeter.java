// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import energy.eddie.api.agnostic.Granularity;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public class MeteringOnHeadpointAndMeter extends Headpoint implements EnergyForGranularity {
    public static final String DISCRIMINATOR_VALUE = "metering-on-headpoint-and-meter";
    @Nullable
    private final List<PhysicalMeter> physicalMeters;
    @Nullable
    private final List<MeasurementSlice> dailyEnergy;
    @Nullable
    private final List<MeasurementSlice> hourlyEnergy;
    @Nullable
    private final List<MeasurementSlice> quarterHourlyEnergy;
    @Nullable
    private final List<SubHeadpoint> subHeadpoints;

    public MeteringOnHeadpointAndMeter(
            @Nullable String ean,
            EnergyType energyType,
            @Nullable List<PhysicalMeter> physicalMeters,
            @Nullable List<MeasurementSlice> dailyEnergy,
            @Nullable List<MeasurementSlice> hourlyEnergy,
            @Nullable List<MeasurementSlice> quarterHourlyEnergy,
            @Nullable List<SubHeadpoint> subHeadpoints
    ) {
        super(DISCRIMINATOR_VALUE, ean, energyType);
        this.physicalMeters = physicalMeters;
        this.dailyEnergy = dailyEnergy;
        this.hourlyEnergy = hourlyEnergy;
        this.quarterHourlyEnergy = quarterHourlyEnergy;
        this.subHeadpoints = subHeadpoints;
    }

    @Override
    public @Nullable List<MeasurementSlice> dailyEnergy() {
        return dailyEnergy;
    }

    @Override
    public @Nullable List<MeasurementSlice> hourlyEnergy() {
        return hourlyEnergy;
    }

    @Override
    public @Nullable List<MeasurementSlice> quarterHourlyEnergy() {
        return quarterHourlyEnergy;
    }

    public @Nullable List<PhysicalMeter> physicalMeters() {
        return physicalMeters;
    }

    public @Nullable List<SubHeadpoint> subHeadpoints() {
        return subHeadpoints;
    }


    @Override
    public boolean dataPresentFor(Granularity granularity) {
        return EnergyForGranularity.super.dataPresentFor(granularity);
    }

    @Override
    public @Nullable ZonedDateTime getLatestMeterReading(Granularity granularity) {
        return getLatestReadingForGranularity(granularity);
    }
}

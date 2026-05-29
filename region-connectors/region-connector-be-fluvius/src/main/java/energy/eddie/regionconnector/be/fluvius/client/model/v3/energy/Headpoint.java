// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class Headpoint implements MeasurementDevice {
    @JsonProperty(value = "$type", required = true)
    protected final String type;
    @Nullable
    protected final String ean;
    protected final EnergyType energyType;
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

    public Headpoint(String ean, EnergyType energyType, List<PhysicalMeter> physicalMeters) {
        this("metering-on-meter", ean, energyType, physicalMeters, null, null, null, null);
    }

    @JsonCreator
    public Headpoint(
            String type, @Nullable String ean, EnergyType energyType,
            @Nullable List<PhysicalMeter> physicalMeters,
            @Nullable List<MeasurementSlice> dailyEnergy,
            @Nullable List<MeasurementSlice> hourlyEnergy,
            @Nullable List<MeasurementSlice> quarterHourlyEnergy,
            @Nullable List<SubHeadpoint> subHeadpoints
    ) {
        this.type = type;
        this.ean = ean;
        this.energyType = energyType;
        this.physicalMeters = physicalMeters;
        this.dailyEnergy = dailyEnergy;
        this.hourlyEnergy = hourlyEnergy;
        this.quarterHourlyEnergy = quarterHourlyEnergy;
        this.subHeadpoints = subHeadpoints;
    }

    public @Nullable ZonedDateTime getEarliestMeterReading(Granularity granularity) {
        var physicalMeterReading = getEarliestMeterReadingOfPhysicalMeters(granularity);
        var subHeadpointReading = getEarliestMeterReadingOfSubHeadpoints(granularity);
        var overallReading = getEarliestReadingForGranularity(granularity);
        return findZonedDateTime(ZonedDateTime::isBefore, physicalMeterReading, subHeadpointReading, overallReading);
    }

    public @Nullable ZonedDateTime getLatestMeterReading(Granularity granularity) {
        var physicalMeterReading = getLatestMeterReadingOfPhysicalMeters(granularity);
        var subHeadpointReading = getLatestMeterReadingOfSubHeadpoints(granularity);
        var overallReading = getLatestReadingForGranularity(granularity);
        return findZonedDateTime(ZonedDateTime::isAfter, physicalMeterReading, subHeadpointReading, overallReading);
    }

    @Nullable
    public String ean() {
        return ean;
    }

    public EnergyType energyType() {
        return energyType;
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

    @Override
    public boolean dataPresentFor(Granularity granularity) {
        return (physicalMeters != null
                && physicalMeters.stream().anyMatch(sub -> sub.dataPresentFor(granularity)))
               || MeasurementDevice.super.dataPresentFor(granularity);
    }

    public List<MeasurementDevice> allMeasurements() {
        var devices = new ArrayList<MeasurementDevice>();
        devices.add(this);
        if (physicalMeters != null) {
            devices.addAll(physicalMeters);
        }
        if (subHeadpoints != null) {
            devices.addAll(subHeadpoints);
        }
        return devices;
    }

    private @Nullable ZonedDateTime getEarliestMeterReadingOfPhysicalMeters(Granularity granularity) {
        return getExtremeMeterReading(granularity,
                                      ZonedDateTime::isBefore,
                                      PhysicalMeter::getEarliestReadingForGranularity,
                                      physicalMeters);
    }

    private @Nullable ZonedDateTime getLatestMeterReadingOfPhysicalMeters(Granularity granularity) {
        return getExtremeMeterReading(granularity,
                                      ZonedDateTime::isAfter,
                                      PhysicalMeter::getLatestReadingForGranularity,
                                      physicalMeters);
    }

    private @Nullable ZonedDateTime getEarliestMeterReadingOfSubHeadpoints(Granularity granularity) {
        return getExtremeMeterReading(granularity,
                                      ZonedDateTime::isBefore,
                                      SubHeadpoint::getLatestReadingForGranularity,
                                      subHeadpoints);
    }

    private @Nullable ZonedDateTime getLatestMeterReadingOfSubHeadpoints(Granularity granularity) {
        return getExtremeMeterReading(granularity,
                                      ZonedDateTime::isAfter,
                                      SubHeadpoint::getLatestReadingForGranularity,
                                      subHeadpoints);
    }

    private <T extends MeasurementDevice> @Nullable ZonedDateTime getExtremeMeterReading(
            Granularity granularity,
            BiPredicate<ZonedDateTime, ZonedDateTime> comparator,
            BiFunction<T, Granularity, ZonedDateTime> readingFunction,
            @Nullable List<T> devices
    ) {
        if (devices == null) {
            return null;
        }
        ZonedDateTime extremeValue = null;
        for (var item : devices) {
            var current = readingFunction.apply(item, granularity);
            if (extremeValue == null || (current != null && comparator.test(current, extremeValue))) {
                extremeValue = current;
            }
        }
        return extremeValue;
    }

    private @Nullable ZonedDateTime findZonedDateTime(
            BiPredicate<ZonedDateTime, ZonedDateTime> comparator,
            @Nullable ZonedDateTime... dates
    ) {
        ZonedDateTime latest = null;
        for (var date : dates) {
            if (date != null && (latest == null || comparator.test(date, latest))) {
                latest = date;
            }
        }
        return latest;
    }
}

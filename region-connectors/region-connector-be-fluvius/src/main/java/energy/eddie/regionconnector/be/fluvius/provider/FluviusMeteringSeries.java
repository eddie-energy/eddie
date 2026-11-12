// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.*;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Interprets the Fluvius metering data of a headpoint independent of the CIM version.
 * Each CIM version maps the resulting series to its own validated historical data documents.
 */
public final class FluviusMeteringSeries {
    private FluviusMeteringSeries() {
        // Utility class
    }

    /**
     * One time series of a headpoint, for a single reading kind and flow direction.
     *
     * @param product      CIM energy product code
     * @param direction    CIM flow direction code
     * @param businessType CIM business type code
     * @param commodity    CIM commodity kind value
     * @param unit         CIM unit of measure code, or null if unknown
     * @param ean          the EAN of the headpoint
     * @param start        start of the measurement slice
     * @param end          end of the measurement slice
     * @param points       the measured values
     */
    public record Series(
            String product,
            String direction,
            @Nullable String businessType,
            String commodity,
            @Nullable String unit,
            String ean,
            ZonedDateTime start,
            ZonedDateTime end,
            List<SeriesPoint> points
    ) {}

    /**
     * @param position starts at 1
     * @param quantity the value converted to the standard unit
     * @param quality  CIM quality code
     */
    public record SeriesPoint(int position, BigDecimal quantity, String quality) {}

    public static List<Series> of(Headpoint headpoint, Granularity granularity) {
        var series = new ArrayList<Series>();
        for (var meter : headpoint.allMeasurements()) {
            var measurementSlices = meter.getForGranularity(granularity);
            if (measurementSlices != null) {
                for (var slice : measurementSlices) {
                    series.addAll(of(headpoint, slice));
                }
            }
        }
        return series;
    }

    @SuppressWarnings("NullAway")
    private static List<Series> of(Headpoint headpoint, MeasurementSlice slice) {
        var series = new ArrayList<Series>();
        for (var kind : Reading.ReadingKind.values()) {
            var product = getProduct(slice, kind);
            if (product == null) {
                continue;
            }
            for (var direction : getFlowDirections(slice.measurements())) {
                var unit = slice.firstUnit(kind, measurement -> getReading(direction, measurement));
                if (unit == null) continue;
                series.add(new Series(
                        product,
                        direction,
                        getBusinessType(direction),
                        getCommodity(headpoint),
                        getUnitOfMeasure(unit),
                        headpoint.ean(),
                        slice.start(),
                        slice.end(),
                        getPoints(slice, kind, unit, direction)
                ));
            }
        }
        return series;
    }

    @SuppressWarnings("NullAway")
    private static List<SeriesPoint> getPoints(
            MeasurementSlice slice,
            Reading.ReadingKind kind,
            Unit unit,
            String direction
    ) {
        var points = new ArrayList<SeriesPoint>();
        var position = 1;
        for (var measurement : slice.measurements()) {
            var reading = getReading(direction, measurement);
            var fallbackTotal = new Total(0.0, unit, null, null);
            reading = reading == null ? new Reading(fallbackTotal) : reading;
            var total = reading.value(kind);
            total = total == null ? fallbackTotal : total;
            var value = total.value() == null
                    ? BigDecimal.ZERO
                    : convertToStandardUnit(total.value(), unit);
            points.add(new SeriesPoint(position, value, getQuality(total)));
            position++;
        }
        return points;
    }

    @Nullable
    private static Reading getReading(String direction, Measurement measurement) {
        return switch (direction) {
            case CimCodes.Direction.UP -> measurement.injection();
            case CimCodes.Direction.DOWN -> measurement.offtake();
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
    }

    private static String getQuality(Total total) {
        return switch (total.validationState()) {
            case READ, VAL -> CimCodes.Quality.AS_PROVIDED;
            case EST, NVAL -> CimCodes.Quality.ESTIMATED;
            case null -> CimCodes.Quality.NOT_AVAILABLE;
        };
    }

    @Nullable
    private static String getUnitOfMeasure(Unit unit) {
        return switch (unit) {
            case UNKNOWN -> null;
            case KVA -> CimCodes.UnitOfMeasure.MEGAVOLT_AMPERE;
            case KVARH -> CimCodes.UnitOfMeasure.MEGAVOLT_AMPERE_REACTIVE_HOURS;
            case KWH -> CimCodes.UnitOfMeasure.KILOWATT_HOUR;
            case KW -> CimCodes.UnitOfMeasure.KILOWATT;
            case M3 -> CimCodes.UnitOfMeasure.CUBIC_METRE;
        };
    }

    private static BigDecimal convertToStandardUnit(double value, Unit unit) {
        return switch (unit) {
            case UNKNOWN, KWH, KW, M3 -> BigDecimal.valueOf(value);
            case KVA, KVARH -> BigDecimal.valueOf(value).divide(BigDecimal.valueOf(1000), RoundingMode.HALF_UP);
        };
    }

    private static String getCommodity(Headpoint headpoint) {
        return switch (headpoint.energyType()) {
            case ELECTRICITY -> CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED;
            case GAS -> CimCodes.Commodity.NATURAL_GAS;
        };
    }

    @Nullable
    private static String getProduct(MeasurementSlice slice, Reading.ReadingKind kind) {
        if (slice.measurements() == null) {
            return null;
        }
        for (var measurement : slice.measurements()) {
            if (readingSupportsKind(kind, measurement.injection())
                || readingSupportsKind(kind, measurement.offtake())) {
                return toProduct(kind);
            }
        }
        return null;
    }

    private static boolean readingSupportsKind(Reading.ReadingKind kind, @Nullable Reading reading) {
        return reading != null && reading.value(kind) != null;
    }

    private static String toProduct(Reading.ReadingKind kind) {
        return switch (kind) {
            case TOTAL -> CimCodes.EnergyProduct.ACTIVE_ENERGY;
            case REACTIVE -> CimCodes.EnergyProduct.REACTIVE_ENERGY;
            case CAPACITIVE -> CimCodes.EnergyProduct.CAPACITIVE_REACTIVE_ENERGY;
            case INDUCTIVE -> CimCodes.EnergyProduct.INDUCTIVE_REACTIVE_ENERGY;
        };
    }

    private static List<String> getFlowDirections(@Nullable List<Measurement> measurements) {
        if (measurements == null) {
            return List.of();
        }
        String direction = null;
        for (var measurement : measurements) {
            if (measurement.supportsOfftake()) {
                if (direction == null) {
                    direction = CimCodes.Direction.DOWN;
                } else if (direction.equals(CimCodes.Direction.UP)) {
                    return List.of(CimCodes.Direction.UP, CimCodes.Direction.DOWN);
                }
            }
            if (measurement.supportsInjection()) {
                if (direction == null) {
                    direction = CimCodes.Direction.UP;
                } else if (direction.equals(CimCodes.Direction.DOWN)) {
                    return List.of(CimCodes.Direction.UP, CimCodes.Direction.DOWN);
                }
            }
        }
        return direction == null ? List.of() : List.of(direction);
    }

    @Nullable
    private static String getBusinessType(String direction) {
        return switch (direction) {
            case CimCodes.Direction.UP -> CimCodes.BusinessType.PRODUCTION;
            case CimCodes.Direction.DOWN -> CimCodes.BusinessType.CONSUMPTION;
            case CimCodes.Direction.UP_AND_DOWN -> CimCodes.BusinessType.NET_PRODUCTION_CONSUMPTION;
            default -> null;
        };
    }
}

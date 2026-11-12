// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.utils.Pair;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading.Reading;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Interprets the readings of an Enedis meter reading independent of the CIM version.
 * Each CIM version maps the resulting series to its own validated historical data documents.
 */
public final class EnedisMeteringSeries {
    private EnedisMeteringSeries() {
        // Utility class
    }

    /**
     * One time series per reading.
     *
     * @param businessType CIM business type code
     * @param product      CIM energy product code, or null if the unit is unknown
     * @param direction    CIM flow direction code
     * @param unit         CIM unit of measure code, or null if the unit is unknown
     * @param periods      consecutive points with the same resolution
     */
    public record Series(
            String businessType,
            @Nullable String product,
            String direction,
            @Nullable String unit,
            List<Period> periods
    ) {}

    /**
     * @param resolution the resolution of the points
     * @param start      start of the period, already adjusted for the Enedis timestamp semantics
     * @param end        end of the period, already adjusted for the Enedis timestamp semantics
     * @param points     the measured values
     */
    public record Period(Duration resolution, ZonedDateTime start, ZonedDateTime end, List<SeriesPoint> points) {}

    /**
     * @param position starts at 1 for every period
     * @param quantity the measured value, converted to kilo units for energy
     * @param quality  CIM quality code
     */
    public record SeriesPoint(int position, BigDecimal quantity, String quality) {}

    public static List<Series> of(MeterReading meterReading) {
        var series = new ArrayList<Series>();
        for (var reading : meterReading.readings()) {
            var convertToKilo = "Wh".equals(reading.unit());
            series.add(new Series(
                    businessType(reading),
                    energyProduct(reading),
                    direction(reading),
                    unit(reading),
                    periods(meterReading, reading, convertToKilo)
            ));
        }
        return series;
    }

    private static String businessType(Reading reading) {
        return switch (reading.direction()) {
            case CONSUMPTION -> CimCodes.BusinessType.CONSUMPTION;
            case PRODUCTION -> CimCodes.BusinessType.PRODUCTION;
        };
    }

    @Nullable
    private static String energyProduct(Reading reading) {
        return switch (reading.unit()) {
            case "W", "VA" -> CimCodes.EnergyProduct.ACTIVE_POWER;
            case "Wh" -> CimCodes.EnergyProduct.ACTIVE_ENERGY;
            default -> null;
        };
    }

    @Nullable
    private static String unit(Reading reading) {
        return switch (reading.unit()) {
            case "W", "VA" -> CimCodes.UnitOfMeasure.WATT;
            case "Wh" -> CimCodes.UnitOfMeasure.KILOWATT_HOUR;
            default -> null;
        };
    }

    private static String direction(Reading reading) {
        return switch (reading.direction()) {
            case CONSUMPTION -> CimCodes.Direction.DOWN;
            case PRODUCTION -> CimCodes.Direction.UP;
        };
    }

    private static List<Period> periods(MeterReading meterReading, Reading reading, boolean convertToKilo) {
        var periods = new ArrayList<Period>();
        for (var chunk : batchIntoChunks(meterReading, reading, convertToKilo)) {
            var resolution = chunk.resolution();
            var start = chunk.start();
            // We need to subtract the resolution from the start date for load curves (date represents the end of the measurement)
            if (resolution.toMinutes() <= 60) {
                start = start.minusMinutes(resolution.toMinutes());
            }
            var end = chunk.end();
            // for daily resolution, we need to adjust the end date (date represents the whole day)
            if (resolution.equals(Duration.ofDays(1))) {
                end = end.plusMinutes(resolution.toMinutes());
            }
            periods.add(new Period(resolution, start, end, chunk.points()));
        }
        return periods;
    }

    private static List<Period> batchIntoChunks(MeterReading meterReading, Reading reading, boolean convertToKilo) {
        var points = reading.points();
        if (points.isEmpty()) {
            return List.of();
        }
        var prevResolution = resolutionOf(meterReading, points.getFirst());
        var position = 1;
        var currentChunk = new ArrayList<>(List.of(new Pair<>(position++, points.getFirst())));
        var chunks = new ArrayList<Period>();
        for (int i = 1; i < points.size(); i++) {
            var cur = points.get(i);
            var curResolution = resolutionOf(meterReading, cur);
            // Different interval length, start new chunk
            if (!curResolution.equals(prevResolution)) {
                chunks.add(chunk(convertToKilo, currentChunk, prevResolution));
                currentChunk = new ArrayList<>();
                position = 1;
                prevResolution = curResolution;
            }
            currentChunk.add(new Pair<>(position++, cur));
        }
        chunks.add(chunk(convertToKilo, currentChunk, prevResolution));
        return chunks;
    }

    private static Duration resolutionOf(MeterReading meterReading, MeterReading.Point point) {
        return point.granularity()
                    .orElseGet(() -> meterReading.granularity()
                                                 .orElse(Granularity.P1D.duration()));
    }

    private static Period chunk(
            boolean convertToKilo,
            List<Pair<Integer, MeterReading.Point>> currentChunk,
            Duration resolution
    ) {
        var points = currentChunk.stream()
                                 .map(point -> toPoint(point, convertToKilo))
                                 .toList();
        return new Period(resolution,
                          currentChunk.getFirst().value().timestamp(),
                          currentChunk.getLast().value().timestamp(),
                          points);
    }

    private static SeriesPoint toPoint(Pair<Integer, MeterReading.Point> point, boolean convertToKilo) {
        var quantity = point.value().value();
        if (convertToKilo) {
            quantity = quantity / 1000.0;
        }
        return new SeriesPoint(point.key(), BigDecimal.valueOf(quantity), CimCodes.Quality.AS_PROVIDED);
    }
}

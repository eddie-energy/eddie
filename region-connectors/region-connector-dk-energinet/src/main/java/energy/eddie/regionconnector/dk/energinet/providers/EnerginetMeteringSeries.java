// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers;

import energy.eddie.regionconnector.dk.energinet.customer.model.Period;
import energy.eddie.regionconnector.dk.energinet.customer.model.Point;
import energy.eddie.regionconnector.dk.energinet.filter.EnerginetResolution;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Interprets the Energinet metering data independent of the CIM version.
 * Codes that are validated against a CIM code list use a predicate, so that each CIM version validates against its own code list.
 */
public final class EnerginetMeteringSeries {
    private EnerginetMeteringSeries() {
        // Utility class
    }

    /**
     * @param resolution ISO 8601 duration of the resolution, or null if unknown
     * @param start      start of the period, formatted as ESMP date time, or null if unknown
     * @param end        end of the period, formatted as ESMP date time, or null if unknown
     * @param points     the measured values
     */
    public record SeriesPeriod(
            @Nullable String resolution,
            @Nullable String start,
            @Nullable String end,
            List<SeriesPoint> points
    ) {}

    /**
     * @param position the position of the point, starts at 1
     * @param quantity the measured value
     * @param quality  CIM quality code
     */
    public record SeriesPoint(int position, BigDecimal quantity, String quality) {}

    /**
     * @param periods        the periods of a time series
     * @param isKnownQuality checks whether a quality code is part of the CIM code list
     */
    public static List<SeriesPeriod> periods(List<Period> periods, Predicate<String> isKnownQuality) {
        var result = new ArrayList<SeriesPeriod>();
        for (var period : periods) {
            var pointList = Optional.ofNullable(period.getPoint()).orElse(List.of());
            var resolution = period.getResolution() == null
                    ? null
                    : new EnerginetResolution(period.getResolution()).toISO8601Duration();
            var timeInterval = period.getTimeInterval();
            String start = null;
            String end = null;
            if (timeInterval != null) {
                var interval = new EsmpTimeInterval(timeInterval.getStart(), timeInterval.getEnd());
                start = interval.start();
                end = interval.end();
            }
            result.add(new SeriesPeriod(resolution, start, end, points(pointList, isKnownQuality)));
        }
        return result;
    }

    /**
     * @param businessType        the business type of the time series
     * @param isKnownBusinessType checks whether a business type code is part of the CIM code list
     * @return the business type, or null if it is unknown
     */
    @Nullable
    public static String businessType(@Nullable String businessType, Predicate<String> isKnownBusinessType) {
        return businessType != null && isKnownBusinessType.test(businessType) ? businessType : null;
    }

    /**
     * @param businessType a CIM business type code
     * @return the CIM flow direction code
     */
    public static String direction(@Nullable String businessType) {
        return switch (businessType) {
            case CimCodes.BusinessType.CONSUMPTION -> CimCodes.Direction.DOWN;
            case CimCodes.BusinessType.PRODUCTION -> CimCodes.Direction.UP;
            case null, default -> CimCodes.Direction.UP_AND_DOWN;
        };
    }

    private static List<SeriesPoint> points(List<Point> pointList, Predicate<String> isKnownQuality) {
        var list = new ArrayList<SeriesPoint>();
        var i = 1;
        for (var point : pointList) {
            var outQuantityQuantity = point.getOutQuantityQuantity() == null
                    ? 0
                    : Double.parseDouble(point.getOutQuantityQuantity());
            var position = point.getPosition();
            var quality = point.getOutQuantityQuality();
            list.add(new SeriesPoint(
                    position == null ? i : Integer.parseInt(position),
                    BigDecimal.valueOf(outQuantityQuantity),
                    quality != null && isKnownQuality.test(quality) ? quality : CimCodes.Quality.AS_PROVIDED
            ));
            i++;
        }
        return list;
    }
}

// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.cim.UsageSegment;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

/**
 * Interprets the CDS usage segments independent of the CIM version, starting with CIM v1.04.
 * Each CIM version maps the results to its own validated historical data documents.
 */
public final class CdsMeteringSeries {
    public static final BigDecimal GAL_TO_CUBIC_METRE = BigDecimal.valueOf(0.00378541);
    public static final BigDecimal FT3_TO_CUBIC_METRE = BigDecimal.valueOf(0.0283168);
    public static final BigDecimal THERM_TO_KWH = BigDecimal.valueOf(29.3001);
    public static final BigDecimal CCF_TO_KWH = THERM_TO_KWH;
    public static final BigDecimal MCF_TO_KWH = THERM_TO_KWH.scaleByPowerOfTen(1);
    public static final BigDecimal MMBTU_TO_KWH = BigDecimal.valueOf(293);

    private CdsMeteringSeries() {
        // Utility class
    }

    /**
     * @param position the position of the value in the usage segment, starts at 1
     * @param quantity the value converted to the CIM unit
     * @param quality  CIM quality code
     */
    public record SeriesPoint(int position, BigDecimal quantity, String quality) {}

    /**
     * @return the CIM unit of measure code of the format
     */
    public static String unit(FormatEnum format) {
        return conversion(format).unit();
    }

    /**
     * Converts a value of the format to the CIM unit.
     */
    public static BigDecimal convert(FormatEnum format, BigDecimal value) {
        return conversion(format).factor().multiply(value);
    }

    /**
     * @return the CIM commodity kind value of the format
     */
    public static String commodity(FormatEnum format) {
        return switch (format) {
            case KWH_NET, KWH_FWD, KWH_REV, USAGE_KWH, USAGE_FWD_KWH, USAGE_REV_KWH, USAGE_NET_KWH, AGGREGATED_KWH,
                 DEMAND_KW, SUPPLY_MIX -> CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED;
            case WATER_M3, WATER_GAL, WATER_FT3 -> CimCodes.Commodity.POTABLE_WATER;
            case GAS_THERM, GAS_CCF, GAS_MCF, GAS_MMBTU -> CimCodes.Commodity.NATURAL_GAS;
            case EACS -> CimCodes.Commodity.NONE;
        };
    }

    /**
     * @return the CIM flow direction code of the format
     */
    public static String direction(FormatEnum format) {
        return switch (format) {
            case KWH_NET, USAGE_KWH, USAGE_NET_KWH -> CimCodes.Direction.UP_AND_DOWN;
            case KWH_REV, USAGE_REV_KWH -> CimCodes.Direction.UP;
            default -> CimCodes.Direction.DOWN;
        };
    }

    /**
     * @return the CIM business type code of a CIM flow direction code, or null for netted values
     */
    @Nullable
    public static String businessType(String direction) {
        return switch (direction) {
            case CimCodes.Direction.UP -> CimCodes.BusinessType.PRODUCTION;
            case CimCodes.Direction.DOWN -> CimCodes.BusinessType.CONSUMPTION;
            default -> null;
        };
    }

    /**
     * @return the resolution of the usage segment
     */
    public static Duration resolution(UsageSegment usageSegment) {
        return Duration.ofSeconds(usageSegment.interval().longValue());
    }

    /**
     * Converts the values of a usage segment, which are within the permission request's timeframe.
     */
    public static List<SeriesPoint> points(
            CdsPermissionRequest permissionRequest,
            UsageSegment usageSegment,
            List<BigDecimal> values,
            FormatEnum format
    ) {
        List<SeriesPoint> points = new ArrayList<>();
        var start = permissionRequest.start().atStartOfDay(ZoneOffset.UTC);
        var end = endOfDay(permissionRequest.end(), ZoneOffset.UTC);
        for (var i = 0; i < values.size(); i++) {
            var offset = usageSegment.interval().multiply(BigDecimal.valueOf(i)).longValue();
            var dateTime = usageSegment.start().toInstant().plusSeconds(offset).atZone(ZoneOffset.UTC);
            if (dateTime.isAfter(end) || dateTime.isBefore(start)) {
                continue;
            }
            points.add(new SeriesPoint(i + 1, convert(format, values.get(i)), CimCodes.Quality.AS_PROVIDED));
        }
        return points;
    }

    private static Conversion conversion(FormatEnum format) {
        return switch (format) {
            case KWH_NET, KWH_FWD, KWH_REV, USAGE_KWH, USAGE_FWD_KWH, USAGE_REV_KWH, USAGE_NET_KWH, AGGREGATED_KWH ->
                    new Conversion(CimCodes.UnitOfMeasure.KILOWATT_HOUR, BigDecimal.ONE);
            case DEMAND_KW -> new Conversion(CimCodes.UnitOfMeasure.KILOWATT, BigDecimal.ONE);
            case GAS_MMBTU -> new Conversion(CimCodes.UnitOfMeasure.KILOWATT_HOUR, MMBTU_TO_KWH);
            case GAS_MCF -> new Conversion(CimCodes.UnitOfMeasure.KILOWATT_HOUR, MCF_TO_KWH);
            case GAS_CCF -> new Conversion(CimCodes.UnitOfMeasure.KILOWATT_HOUR, CCF_TO_KWH);
            case GAS_THERM -> new Conversion(CimCodes.UnitOfMeasure.KILOWATT_HOUR, THERM_TO_KWH);
            case WATER_FT3 -> new Conversion(CimCodes.UnitOfMeasure.CUBIC_METRE, FT3_TO_CUBIC_METRE);
            case WATER_GAL -> new Conversion(CimCodes.UnitOfMeasure.CUBIC_METRE, GAL_TO_CUBIC_METRE);
            case WATER_M3 -> new Conversion(CimCodes.UnitOfMeasure.CUBIC_METRE, BigDecimal.ONE);
            case SUPPLY_MIX, EACS -> new Conversion(CimCodes.UnitOfMeasure.ONE, BigDecimal.ONE);
        };
    }

    private record Conversion(String unit, BigDecimal factor) {}
}

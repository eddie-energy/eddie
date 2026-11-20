// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers;

import com.rometools.rome.feed.synd.SyndEntry;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import jakarta.annotation.Nullable;
import org.naesb.espi.IntervalBlock;
import org.naesb.espi.ReadingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Interprets a Green Button feed independent of the CIM version.
 * Each CIM version maps the resulting readings to its own validated historical data documents.
 */
public final class GreenButtonMeteringSeries {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreenButtonMeteringSeries.class);

    private GreenButtonMeteringSeries() {
        // Utility class
    }

    /**
     * A reading type together with its interval block.
     *
     * @param readingType        describes the unit, direction and commodity of the readings
     * @param intervalBlock      contains the readings
     * @param intervalBlockEntry the feed entry of the interval block, identifies the usage point
     */
    public record IntervalReadings(ReadingType readingType, IntervalBlock intervalBlock, SyndEntry intervalBlockEntry) {}

    /**
     * One reading of an interval block.
     *
     * @param start      start of the reading, formatted as ESMP date time
     * @param end        end of the reading, formatted as ESMP date time
     * @param resolution the interval length of the reading type
     * @param value      the scaled value
     */
    public record Period(String start, String end, Duration resolution, BigDecimal value) {}

    /**
     * Finds all reading types of the feed, which have a parseable interval block.
     */
    public static List<IntervalReadings> intervalReadings(Query query, String permissionId) {
        var readings = query.findAllByTitle("ReadingType");
        var result = new ArrayList<IntervalReadings>(readings.size());
        for (var reading : readings) {
            var related = reading.findRelatedLink("related").getHref();
            var optionalIntervalBlock = query.findFirstBySelfLinkAndTitle(related, "IntervalBlock");
            var readingType = query.unmarshal(reading, ReadingType.class);

            if (optionalIntervalBlock.isEmpty() || readingType == null) {
                LOGGER.warn("Could not parse interval block for reading type {} for permission request {}",
                            related,
                            permissionId);
                continue;
            }

            var intervalBlockEntry = optionalIntervalBlock.get();
            var intervalBlock = query.unmarshal(intervalBlockEntry, IntervalBlock.class);
            if (intervalBlock == null) {
                LOGGER.warn("Could not parse interval block {} for permission request {}", related, permissionId);
            } else {
                result.add(new IntervalReadings(readingType, intervalBlock, intervalBlockEntry));
            }
        }
        return result;
    }

    public static List<Period> periods(IntervalReadings intervalReadings, MeasurementScale<?> scale) {
        var readingType = intervalReadings.readingType();
        var granularity = Granularity.fromMinutes(Math.toIntExact(readingType.getIntervalLength() / 60));
        List<Period> periods = new ArrayList<>();
        for (var intervalReading : intervalReadings.intervalBlock().getIntervalReading()) {
            var timePeriod = intervalReading.getTimePeriod();
            periods.add(new Period(
                    Instant.ofEpochSecond(timePeriod.getStart()).atZone(ZoneOffset.UTC).toString(),
                    Instant.ofEpochSecond(timePeriod.getStart() + timePeriod.getDuration())
                           .atZone(ZoneOffset.UTC)
                           .toString(),
                    granularity.duration(),
                    scale.scaled(intervalReading.getValue())
            ));
        }
        return periods;
    }

    /**
     * @return the CIM commodity kind value, or null if the commodity is not supported
     */
    @Nullable
    public static String commodity(ReadingType readingType) {
        return switch (readingType.getCommodity()) {
            case "2" -> CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED;
            case "1" -> CimCodes.Commodity.ELECTRICITY_SECONDARY_METERED;
            default -> null;
        };
    }

    /**
     * @return the CIM flow direction code, or null if the direction is not supported
     */
    @Nullable
    public static String direction(ReadingType readingType) {
        return switch (readingType.getFlowDirection()) {
            case "1" -> CimCodes.Direction.DOWN;
            case "19" -> CimCodes.Direction.UP;
            case "4" -> CimCodes.Direction.UP_AND_DOWN;
            default -> null;
        };
    }

    /**
     * @return the CIM business type code for a CIM flow direction code
     */
    @Nullable
    public static String businessType(@Nullable String direction) {
        return switch (direction) {
            case CimCodes.Direction.UP -> CimCodes.BusinessType.PRODUCTION;
            case CimCodes.Direction.DOWN -> CimCodes.BusinessType.CONSUMPTION;
            case CimCodes.Direction.UP_AND_DOWN -> CimCodes.BusinessType.NET_PRODUCTION_CONSUMPTION;
            case null, default -> null;
        };
    }
}

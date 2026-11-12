// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Interprets a single ETA Plus historical-readings response independent of the CIM version.
 * Per the ETA Plus contract, {@code unit} and {@code direction} are constant within a response,
 * so each response yields at most one series.
 */
public final class EtaPlusMeteringSeries {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusMeteringSeries.class);

    private EtaPlusMeteringSeries() {
        // Utility class
    }

    /**
     * @param meteringPointId the metering point of the response
     * @param unit            CIM unit of measure code
     * @param businessType    CIM business type code
     * @param direction       CIM flow direction code
     * @param commodity       CIM commodity kind value
     * @param product         CIM energy product code, only set for electricity
     * @param start           timestamp of the first reading
     * @param end             timestamp of the last reading
     * @param points          the readings sorted by their timestamp
     */
    public record Series(
            String meteringPointId,
            String unit,
            String businessType,
            String direction,
            String commodity,
            @Nullable String product,
            ZonedDateTime start,
            ZonedDateTime end,
            List<SeriesPoint> points
    ) {}

    /**
     * @param position starts at 1
     * @param quantity the measured value
     * @param quality  CIM quality code, or null if the reading status is unknown
     */
    public record SeriesPoint(int position, BigDecimal quantity, @Nullable String quality) {}

    /**
     * @return the series, or empty if the response contains no readings or an unsupported unit
     */
    public static Optional<Series> of(IdentifiableValidatedHistoricalData identifiableData) {
        var payload = identifiableData.payload();
        var readings = payload.readings();
        if (readings == null || readings.isEmpty()) {
            return Optional.empty();
        }
        var firstReading = readings.getFirst();
        String unit;
        try {
            unit = translateUnit(firstReading.unit());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Skipping VHD for metering point {}: unsupported unit '{}'",
                        payload.meteringPointId(), firstReading.unit());
            return Optional.empty();
        }
        var sortedReadings = EtaPlusVhdMappings.sortByTimestamp(readings);
        var energyType = identifiableData.permissionRequest().energyType();
        var direction = firstReading.direction();
        return Optional.of(new Series(
                payload.meteringPointId(),
                unit,
                businessTypeFor(direction, payload.meteringPointId()),
                flowDirectionFor(direction),
                commodityFor(energyType),
                productFor(energyType),
                sortedReadings.getFirst().timestamp(),
                sortedReadings.getLast().timestamp(),
                points(sortedReadings, payload.meteringPointId())
        ));
    }

    private static List<SeriesPoint> points(List<EtaPlusMeteredData.MeterReading> sortedReadings, String meteringPointId) {
        var points = new ArrayList<SeriesPoint>(sortedReadings.size());
        for (int i = 0; i < sortedReadings.size(); i++) {
            var reading = sortedReadings.get(i);
            points.add(new SeriesPoint(i + 1,
                                       BigDecimal.valueOf(reading.value()),
                                       qualityFor(reading.quality(), meteringPointId)));
        }
        return points;
    }

    private static String commodityFor(EnergyType energyType) {
        return switch (energyType) {
            case ELECTRICITY -> CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED;
            case NATURAL_GAS -> CimCodes.Commodity.NATURAL_GAS;
            default -> throw new IllegalStateException("Unsupported energy type: " + energyType);
        };
    }

    private static String businessTypeFor(String direction, String meteringPointId) {
        if (EtaPlusVhdMappings.isProduction(direction)) {
            return CimCodes.BusinessType.PRODUCTION;
        }
        if (!"Consumption".equals(direction)) {
            LOGGER.warn("Unknown direction '{}' on metering point {}; defaulting to CONSUMPTION",
                        direction, meteringPointId);
        }
        return CimCodes.BusinessType.CONSUMPTION;
    }

    private static String flowDirectionFor(String direction) {
        return EtaPlusVhdMappings.isProduction(direction)
                ? CimCodes.Direction.UP
                : CimCodes.Direction.DOWN;
    }

    /**
     * Q-2 (L1): the product is only set for electricity, there is no gas-appropriate value.
     */
    @Nullable
    private static String productFor(EnergyType energyType) {
        return energyType == EnergyType.ELECTRICITY ? CimCodes.EnergyProduct.ACTIVE_ENERGY : null;
    }

    @Nullable
    private static String qualityFor(String wireStatus, String meteringPointId) {
        if (EtaPlusVhdMappings.isValidatedStatus(wireStatus)) {
            return CimCodes.Quality.AS_PROVIDED;
        }
        LOGGER.info("Unknown reading status '{}' on metering point {}; omitting quality",
                    wireStatus, meteringPointId);
        return null;
    }

    private static String translateUnit(@Nullable String wireUnit) {
        if (wireUnit == null) {
            throw new IllegalArgumentException("null");
        }
        return switch (wireUnit) {
            case "kWh", "KWH" -> CimCodes.UnitOfMeasure.KILOWATT_HOUR;
            case "MWh", "MWH" -> CimCodes.UnitOfMeasure.MEGAWATT_HOURS;
            case "m³", "m3", "M3" -> CimCodes.UnitOfMeasure.CUBIC_METRE;
            default -> throw new IllegalArgumentException(wireUnit);
        };
    }
}

// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider;

import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interprets the energy data of an EDA consumption record independent of the CIM version.
 * Units are validated with a predicate, so that each CIM version validates against its own code lists.
 */
public final class EdaMeteringSeries {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaMeteringSeries.class);

    private EdaMeteringSeries() {
        // Utility class
    }

    /**
     * @param position starts at 1
     * @param quantity the billing quantity
     * @param quality  CIM quality code, or null if the metering method is unknown
     */
    public record SeriesPoint(int position, BigDecimal quantity, @Nullable String quality) {}

    /**
     * @param energyData  the energy data of a meter
     * @param messageId   the ID of the consumption record, used for logging
     * @param isKnownUnit checks whether a unit is part of the CIM code lists
     * @return the billing unit, or null if it is not part of the CIM code lists
     */
    @Nullable
    public static String measurementUnit(EnergyData energyData, String messageId, Predicate<String> isKnownUnit) {
        var billingUnit = energyData.billingUnit();
        if (billingUnit != null && isKnownUnit.test(billingUnit)) {
            return billingUnit;
        }
        LOGGER.atDebug()
              .addArgument(billingUnit)
              .addArgument(messageId)
              .log("Got unknown unit {} for message {}");
        return null;
    }

    public static List<SeriesPoint> points(EnergyData energyData, String messageId) {
        List<SeriesPoint> points = new ArrayList<>();
        for (int i = 0; i < energyData.energyPositions().size(); i++) {
            var meterReading = energyData.energyPositions().get(i);
            // Position has to be a positive integer != 0
            points.add(new SeriesPoint(i + 1, meterReading.billingQuantity(), quality(meterReading, messageId)));
        }
        return points;
    }

    @Nullable
    private static String quality(EnergyPosition meterReading, String messageId) {
        var meteringMethod = meterReading.meteringMethod();
        return switch (meteringMethod) {
            case "L1" -> CimCodes.Quality.AS_PROVIDED;
            case "L2" -> CimCodes.Quality.ADJUSTED;
            case "L3" -> CimCodes.Quality.ESTIMATED;
            default -> {
                LOGGER.info("Unknown metering method {} for message {}", meteringMethod, messageId);
                yield null;
            }
        };
    }
}

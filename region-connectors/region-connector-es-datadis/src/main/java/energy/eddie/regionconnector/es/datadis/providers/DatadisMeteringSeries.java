// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.CimCodes;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Interprets the Datadis metering data independent of the CIM version.
 * Each CIM version maps the resulting series to its own validated historical data documents.
 */
public final class DatadisMeteringSeries {
    private DatadisMeteringSeries() {
        // Utility class
    }

    /**
     * One time series per CUPS and flow direction.
     *
     * @param businessType CIM business type code
     * @param direction    CIM flow direction code
     * @param cups         the CUPS of the metering data
     * @param points       the measured values
     */
    public record Series(String businessType, String direction, String cups, List<SeriesPoint> points) {}

    /**
     * @param position the position of the point in the metering data, starts at 1
     * @param quantity the measured value in kWh
     * @param quality  CIM quality code
     */
    public record SeriesPoint(int position, BigDecimal quantity, String quality) {}

    /**
     * Creates the consumption series and, if the permission request supports it, the production series.
     */
    public static List<Series> of(IdentifiableMeteringData identifiableMeteringData) {
        var list = new ArrayList<>(series(
                identifiableMeteringData,
                CimCodes.BusinessType.CONSUMPTION,
                CimCodes.Direction.DOWN,
                MeteringData::consumptionKWh
        ));
        if (identifiableMeteringData.permissionRequest().productionSupport()) {
            list.addAll(series(
                    identifiableMeteringData,
                    CimCodes.BusinessType.PRODUCTION,
                    CimCodes.Direction.UP,
                    MeteringData::surplusEnergyKWh
            ));
        }
        return list;
    }

    /**
     * @return the resolution of the metering data, based on the measurement type of the permission request
     */
    public static Duration resolution(IdentifiableMeteringData identifiableMeteringData) {
        var granularity = switch (identifiableMeteringData.permissionRequest().measurementType()) {
            case MeasurementType.QUARTER_HOURLY -> Granularity.PT15M;
            case MeasurementType.HOURLY -> Granularity.PT1H;
        };
        return granularity.duration();
    }

    private static List<Series> series(
            IdentifiableMeteringData identifiableMeteringData,
            String businessType,
            String direction,
            ToDoubleFunction<MeteringData> energyAccessor
    ) {
        Map<String, List<SeriesPoint>> points = new HashMap<>();
        int position = 1;
        for (MeteringData meteringData : identifiableMeteringData.intermediateMeteringData().meteringData()) {
            points.computeIfAbsent(meteringData.cups(), cups -> new ArrayList<>())
                  .add(new SeriesPoint(position,
                                       BigDecimal.valueOf(energyAccessor.applyAsDouble(meteringData)),
                                       quality(meteringData)));
            position++;
        }
        var series = new ArrayList<Series>();
        for (var cups : points.entrySet()) {
            series.add(new Series(businessType, direction, cups.getKey(), cups.getValue()));
        }
        return series;
    }

    /**
     * Maps the metering data obtain method to a CIM quality code.
     *
     * @param meteringData the metering data
     * @return estimated if the obtain method is "Estimada", not available if it is unknown, otherwise as provided
     */
    private static String quality(MeteringData meteringData) {
        return switch (meteringData.obtainMethod()) {
            case ESTIMATED -> CimCodes.Quality.ESTIMATED;
            case REAL -> CimCodes.Quality.AS_PROVIDED;
            case UNKNOWN -> CimCodes.Quality.NOT_AVAILABLE;
        };
    }
}

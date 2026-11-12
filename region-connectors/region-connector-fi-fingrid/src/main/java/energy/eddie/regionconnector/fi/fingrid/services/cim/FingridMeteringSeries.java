// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim;

import energy.eddie.regionconnector.fi.fingrid.client.model.Observation;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeries;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesData;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Interprets the Fingrid time series independent of the CIM version.
 * Each CIM version maps the resulting periods to its own validated historical data documents.
 */
public final class FingridMeteringSeries {
    private FingridMeteringSeries() {
        // Utility class
    }

    /**
     * One period per Fingrid time series.
     *
     * @param start      start of the period, formatted as ESMP date time
     * @param end        end of the period, formatted as ESMP date time
     * @param resolution the resolution of the observations
     * @param points     the observations
     */
    public record Period(String start, String end, Duration resolution, List<SeriesPoint> points) {}

    /**
     * @param position starts at 1
     * @param quantity the observed value
     * @param quality  CIM quality code
     */
    public record SeriesPoint(int position, BigDecimal quantity, String quality) {}

    public static List<Period> periods(List<TimeSeries> timeSeries) {
        var list = new ArrayList<Period>();
        for (var series : timeSeries) {
            var interval = new EsmpTimeInterval(series.start(), series.end());
            list.add(new Period(interval.start(),
                                interval.end(),
                                series.resolutionDuration().duration(),
                                points(series)));
        }
        return list;
    }

    /**
     * @return the event reasons of the data separated by line breaks, or null if there are none
     */
    @Nullable
    public static String reasons(TimeSeriesData data) {
        var eventReasons = data.transaction().eventReasons();
        if (eventReasons == null || eventReasons.reasons().isEmpty()) {
            return null;
        }
        var reasons = new StringBuilder();
        for (var reason : eventReasons.reasons()) {
            reasons.append(reason.eventReasonText())
                   .append("\n");
        }
        return reasons.toString();
    }

    private static List<SeriesPoint> points(TimeSeries series) {
        List<SeriesPoint> result = new ArrayList<>();
        var position = 1;
        for (Observation obs : series.observations()) {
            var quality = obs.quality().equals("OK") ? CimCodes.Quality.AS_PROVIDED : CimCodes.Quality.NOT_AVAILABLE;
            result.add(new SeriesPoint(position, obs.quantity(), quality));
            position++;
        }
        return result;
    }
}

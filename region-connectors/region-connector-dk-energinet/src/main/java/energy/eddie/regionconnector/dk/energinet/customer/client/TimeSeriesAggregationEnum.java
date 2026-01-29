// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;

import java.time.LocalDate;

/**
 * Enum for the Aggregation of MeterDataApi's TimeSeries Method
 *
 * @see energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient
 * @see energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient#getTimeSeries(LocalDate, LocalDate, Granularity, MeteringPointsRequest, String)
 */
enum TimeSeriesAggregationEnum {
    ACTUAL("Actual"),
    QUARTER("Quarter"),
    HOUR("Hour"),
    DAY("Day"),
    MONTH("Month"),
    YEAR("Year");

    private final String aggregation;

    TimeSeriesAggregationEnum(String aggregation) {
        this.aggregation = aggregation;
    }

    public static TimeSeriesAggregationEnum fromGranularity(Granularity granularity) {
        return switch (granularity) {
            case PT15M -> QUARTER;
            case PT1H -> HOUR;
            case P1D -> DAY;
            case P1M -> MONTH;
            case P1Y -> YEAR;
            default -> throw new IllegalArgumentException("Unsupported granularity: " + granularity);
        };
    }

    @Override
    public String toString() {
        return aggregation;
    }
}
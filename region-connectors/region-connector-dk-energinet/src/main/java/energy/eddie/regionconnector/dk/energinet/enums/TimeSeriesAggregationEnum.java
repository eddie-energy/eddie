package energy.eddie.regionconnector.dk.energinet.enums;

import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;

import java.util.Objects;

/**
 * Enum for the Aggregation of MeterDataApi's TimeSeries Method
 *
 * @see MeterDataApi
 * @see MeterDataApi#apiMeterdataGettimeseriesDateFromDateToAggregationPost(String, String, String, MeteringPointsRequest)
 *
 */
public enum TimeSeriesAggregationEnum {
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

    public static TimeSeriesAggregationEnum fromString(String aggregation) {
        var sanitisedAggregation = sanitiseAggregationInput(aggregation);

        for (var aggregationEnum : values()) {
            if (aggregationEnum.toString().equals(sanitisedAggregation)) {
                return aggregationEnum;
            }
        }

        throw new IllegalArgumentException("Invalid TimeSeriesAggregationEnum value: " + aggregation);
    }

    private static String sanitiseAggregationInput(String aggregation) {
        if (Objects.requireNonNull(aggregation).isBlank()) {
            throw new IllegalArgumentException("Provided aggregation does not exist.");
        }

        var trimmedAggregation = aggregation.trim();
        String firstLetter = trimmedAggregation.substring(0, 1).toUpperCase();
        String restOfString = trimmedAggregation.substring(1).toLowerCase();

        return firstLetter + restOfString;
    }

    @Override
    public String toString() {
        return aggregation;
    }
}

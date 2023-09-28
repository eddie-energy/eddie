package energy.eddie.regionconnector.dk.energinet.enums;

import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;

import java.util.Objects;
import java.util.UUID;

/**
 * Enum for the Aggregation of MeterDataApi's TimeSeries Method
 *
 * @see MeterDataApi
 * @see MeterDataApi#apiMeterdataGettimeseriesDateFromDateToAggregationPost(String, String, String, UUID, MeteringPointsRequest)
 */
public enum TimeSeriesAggregationEnum {
    ACTUAL("Actual"),
    QUARTER("Quarter"),
    HOUR("Hour"),
    DAY("Day"),
    MONTH("Month"),
    YEAR("Year");

    private static final int MIN_LEN = 2;
    private static final String EXCEPTION_MESSAGE = "Invalid TimeSeriesAggregationEnum value: ";
    private final String aggregation;

    TimeSeriesAggregationEnum(String aggregation) {
        this.aggregation = aggregation;
    }

    public static TimeSeriesAggregationEnum fromString(String aggregation) {
        if (!Objects.requireNonNull(aggregation).isBlank()) {
            var sanitisedAggregation = sanitiseAggregationInput(aggregation);

            for (var aggregationEnum : values()) {
                if (aggregationEnum.toString().equals(sanitisedAggregation)) {
                    return aggregationEnum;
                }
            }
        }

        throw new IllegalArgumentException(EXCEPTION_MESSAGE + aggregation);
    }

    private static String sanitiseAggregationInput(String aggregation) {
        var trimmedAggregation = aggregation.trim();

        if (trimmedAggregation.length() < MIN_LEN) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE + aggregation);
        }

        String firstLetter = trimmedAggregation.substring(0, 1).toUpperCase();
        String restOfString = trimmedAggregation.substring(1).toLowerCase();

        return firstLetter + restOfString;
    }

    @Override
    public String toString() {
        return aggregation;
    }
}

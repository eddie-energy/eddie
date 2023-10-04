package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import io.javalin.validation.JavalinValidation;
import jakarta.annotation.Nullable;
import kotlin.jvm.functions.Function1;

public class TimeSeriesAggregationEnumConverter implements Function1<String, TimeSeriesAggregationEnum> {

    public static void register() {
        JavalinValidation.register(TimeSeriesAggregationEnum.class, new TimeSeriesAggregationEnumConverter());
    }

    @Nullable
    @Override
    public TimeSeriesAggregationEnum invoke(String value) {
        try {
            return value != null && !value.isBlank() ? TimeSeriesAggregationEnum.fromString(value) : null;
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }
}
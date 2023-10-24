package energy.eddie.regionconnector.dk.energinet.enums;

import java.util.Objects;

public enum PeriodResolutionEnum {
    PT15M,
    PT1H,
    PT1D,
    P1M,
    P1Y;

    public static PeriodResolutionEnum fromString(String periodResolution) {
        if (!Objects.requireNonNull(periodResolution).isBlank()) {
            if (periodResolution.equals("P1D")) {
                return PT1D;
            }

            for (var periodResolutionEnum : values()) {
                if (periodResolutionEnum.name().equals(periodResolution)) {
                    return periodResolutionEnum;
                }
            }
        }

        throw new IllegalArgumentException("Invalid PeriodResolutionEnum value" + periodResolution);
    }

    public static PeriodResolutionEnum fromTimeSeriesAggregation(TimeSeriesAggregationEnum timeSeriesAggregationEnum) {
        return switch (timeSeriesAggregationEnum) {
            case ACTUAL, HOUR -> PT1H;
            case QUARTER -> PT15M;
            case DAY -> PT1D;
            case MONTH -> P1M;
            case YEAR -> P1Y;
        };
    }
}

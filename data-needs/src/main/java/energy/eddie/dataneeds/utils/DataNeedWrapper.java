package energy.eddie.dataneeds.utils;

import energy.eddie.dataneeds.needs.TimeframedDataNeed;

import java.time.LocalDate;

public record DataNeedWrapper(
        TimeframedDataNeed timeframedDataNeed,
        LocalDate calculatedStart,
        LocalDate calculatedEnd) {
}

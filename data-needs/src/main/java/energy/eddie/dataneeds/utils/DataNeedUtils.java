package energy.eddie.dataneeds.utils;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.duration.CalendarUnit;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

public class DataNeedUtils {
    private DataNeedUtils() {}

    /**
     * Helper method that evaluates the start and end date of the passed data need.
     *
     * @see DataNeedsService#findDataNeedAndCalculateStartAndEnd(String, LocalDate, Period, Period)
     **/
    public static DataNeedWrapper calculateRelativeStartAndEnd(
            DataNeed dataNeed,
            LocalDate referenceDate,
            Period earliestStart,
            Period latestEnd
    ) throws IllegalArgumentException {
        if (!(dataNeed instanceof TimeframedDataNeed timeframedDataNeed))
            throw new IllegalArgumentException("Data need with id '%s' does not have a duration.".formatted(dataNeed.id()));

        if (timeframedDataNeed.duration() instanceof AbsoluteDuration absoluteDuration)
            return new DataNeedWrapper(timeframedDataNeed, absoluteDuration.start(), absoluteDuration.end());

        var duration = ((RelativeDuration) timeframedDataNeed.duration());
        Optional<Period> durationStart = duration.start();
        Optional<Period> durationEnd = duration.end();
        Optional<CalendarUnit> stickToStart = duration.stickyStartCalendarUnit();

        var calculatedStart = durationStart.map(referenceDate::plus).orElseGet(() -> referenceDate.plus(earliestStart));
        var calculatedEnd = durationEnd.map(referenceDate::plus).orElseGet(() -> referenceDate.plus(latestEnd));

        if (stickToStart.isPresent()) {
            // overwrite start if date need specifies sticky start
            calculatedStart = switch (stickToStart.get()) {
                case CalendarUnit.WEEK -> calculatedStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case CalendarUnit.MONTH -> calculatedStart.with(TemporalAdjusters.firstDayOfMonth());
                case CalendarUnit.YEAR -> calculatedStart.with(TemporalAdjusters.firstDayOfYear());
            };
        }

        return new DataNeedWrapper(timeframedDataNeed, calculatedStart, calculatedEnd);
    }
}

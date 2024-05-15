package energy.eddie.dataneeds.utils;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.duration.CalendarUnit;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

public class TimeframedDataNeedUtils {
    private TimeframedDataNeedUtils() {}

    /**
     * Helper method that evaluates the start and end date of the passed data need.
     * <p>
     * <p>
     * Evaluates the relative dates if the given data need has a relative duration. It uses {@code referenceDate} to
     * calculate the start and end dates, therefore the date should have been created with the timezone of the region
     * connector in mind. {@code earliestStart} and {@code latestEnd} represent the Periods for the earliest/latest date
     * the calling region connector supports and are used if the data need is configured with open start/end. This
     * method also calculates the correct start date if a sticky start is defined by the data needs relative duration.
     *
     * @param timeframedDataNeed Data need to calculate the start and end date for.
     * @param referenceDate      Reference date to use as reference for calculations for relative durations.
     * @param earliestStart      Period indicating the earliest start that is supported by the calling region
     *                           connector.
     * @param latestEnd          Period indicating the latest end that is supported by the calling region connector.
     * @return Wrapper containing the data need and the start and end date.
     **/
    public static DataNeedWrapper calculateRelativeStartAndEnd(
            TimeframedDataNeed timeframedDataNeed,
            LocalDate referenceDate,
            Period earliestStart,
            Period latestEnd
    ) throws IllegalArgumentException, UnsupportedDataNeedException {
        var earliestStartDate = referenceDate.plus(earliestStart);
        var latestEndDate = referenceDate.plus(latestEnd);
        if (timeframedDataNeed.duration() instanceof AbsoluteDuration absoluteDuration) {
            earliestStartIsAfterStartThrows(timeframedDataNeed, earliestStartDate, absoluteDuration.start());
            latestEndIsBeforeEndThrows(timeframedDataNeed, latestEndDate, absoluteDuration.end());
            return new DataNeedWrapper(timeframedDataNeed, absoluteDuration.start(), absoluteDuration.end());
        }

        var duration = ((RelativeDuration) timeframedDataNeed.duration());
        Optional<Period> durationStart = duration.start();
        Optional<Period> durationEnd = duration.end();
        Optional<CalendarUnit> stickToStart = duration.stickyStartCalendarUnit();

        var calculatedStart = durationStart.map(referenceDate::plus).orElse(earliestStartDate);
        var calculatedEnd = durationEnd.map(referenceDate::plus).orElse(latestEndDate);

        if (stickToStart.isPresent()) {
            // overwrite start if date need specifies sticky start
            calculatedStart = switch (stickToStart.get()) {
                case CalendarUnit.WEEK -> calculatedStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case CalendarUnit.MONTH -> calculatedStart.with(TemporalAdjusters.firstDayOfMonth());
                case CalendarUnit.YEAR -> calculatedStart.with(TemporalAdjusters.firstDayOfYear());
            };
        }

        earliestStartIsAfterStartThrows(timeframedDataNeed, earliestStartDate, calculatedStart);
        latestEndIsBeforeEndThrows(timeframedDataNeed, latestEndDate, calculatedEnd);
        return new DataNeedWrapper(timeframedDataNeed, calculatedStart, calculatedEnd);
    }

    private static void earliestStartIsAfterStartThrows(
            TimeframedDataNeed timeframedDataNeed,
            LocalDate earliestStartDate,
            LocalDate start
    ) throws UnsupportedDataNeedException {
        if (earliestStartDate.isAfter(start)) {
            throw new UnsupportedDataNeedException(timeframedDataNeed.id(), "Earliest start is after start");
        }
    }

    private static void latestEndIsBeforeEndThrows(
            TimeframedDataNeed timeframedDataNeed,
            LocalDate latestEndDate,
            LocalDate end
    ) throws UnsupportedDataNeedException {
        if (latestEndDate.isBefore(end)) {
            throw new UnsupportedDataNeedException(timeframedDataNeed.id(), "Latest end is before end");
        }
    }
}

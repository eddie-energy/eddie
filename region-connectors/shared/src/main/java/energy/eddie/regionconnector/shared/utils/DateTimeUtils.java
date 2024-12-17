package energy.eddie.regionconnector.shared.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

public class DateTimeUtils {
    private DateTimeUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime endOfDay(LocalDate localDate, ZoneId zone) {
        return localDate.plusDays(1)
                        .atStartOfDay(zone)
                        .minusSeconds(1);
    }


    /**
     * Finds the oldest date time in a collection of data times.
     * If the collection is empty or only contains null values, returns an empty optional.
     *
     * @param dateTimes the collection of date times
     * @return the oldest datetime or empty optional if the collection is empty, or filled with null values.
     */
    public static Optional<ZonedDateTime> oldestDateTime(Collection<ZonedDateTime> dateTimes) {
        ZonedDateTime oldest = null;
        for (var value : dateTimes) {
            if (oldest == null || value.isBefore(oldest)) {
                oldest = value;
            }
        }
        return Optional.ofNullable(oldest);
    }
}

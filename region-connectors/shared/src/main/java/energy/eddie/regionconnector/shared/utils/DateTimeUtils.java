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

package energy.eddie.regionconnector.shared.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LocalDateUtils {
    private LocalDateUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime endOfDay(LocalDate localDate, ZoneId zone) {
        return localDate.plusDays(1)
                        .atStartOfDay(zone)
                        .minusSeconds(1);
    }
}

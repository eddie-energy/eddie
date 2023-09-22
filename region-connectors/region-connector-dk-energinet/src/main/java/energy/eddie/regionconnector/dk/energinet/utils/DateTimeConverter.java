package energy.eddie.regionconnector.dk.energinet.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter {
    private DateTimeConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime isoDateToZonedDateTime(String isoDate, String zonedId) {
        LocalDate localDate = LocalDate.parse(isoDate, DateTimeFormatter.ISO_DATE);
        LocalTime localTime = LocalTime.of(0, 0, 0, 0);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

        return ZonedDateTime.of(localDateTime, ZoneId.of(zonedId));
    }

    public static ZonedDateTime isoDateTimeToZonedDateTime(String isoDateTime, String zonedId) {
        LocalDateTime localDateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME);

        return ZonedDateTime.of(localDateTime, ZoneId.of(zonedId));
    }
}

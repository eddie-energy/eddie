package energy.eddie.regionconnector.fr.enedis;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter {
    private DateTimeConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime isoDateToZonedDateTime(String isoDate) {
        LocalDate localDate = LocalDate.parse(isoDate, DateTimeFormatter.ISO_DATE);
        LocalTime localTime = LocalTime.of(0, 0, 0, 0);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

        return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
    }
}

package energy.eddie.regionconnector.fr.enedis.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public class DateTimeConverter {
    private DateTimeConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime isoDateToZonedDateTime(String isoDate) {
        LocalDate localDate = LocalDate.parse(isoDate, DateTimeFormatter.ISO_DATE);
        LocalTime localTime = LocalTime.of(0, 0, 0, 0);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

        return ZonedDateTime.of(localDateTime, ZONE_ID_FR);
    }
}

package energy.eddie.regionconnector.fr.enedis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeConverterTest {
    @Test
    public void testIsoDateToZonedDateTime() {
        String isoDate = "2023-06-01";
        ZonedDateTime expected = ZonedDateTime.of(2023, 6, 1, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        Assertions.assertEquals(expected, DateTimeConverter.isoDateToZonedDateTime(isoDate));
    }

    @Test
    public void testWrongDateFormat() {
        String isoDate = "01-06-2023";

        Assertions.assertThrows(DateTimeException.class, () -> DateTimeConverter.isoDateToZonedDateTime(isoDate));
    }
}

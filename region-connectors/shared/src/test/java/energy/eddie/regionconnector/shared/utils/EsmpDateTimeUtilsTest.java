package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EsmpDateTimeUtilsTest {

    @Test
    void zonedDateTimeToESMPDateTimeString_withUTCOffset_producesExpectedOutput() {

        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneId.of("Europe/Vienna"));
        String expected = "2023-01-01T00:01Z";

        String result = EsmpDateTimeUtils.zonedDateTimeToESMPDateTimeString(zonedDateTime);

        assertEquals(expected, result);
    }
}
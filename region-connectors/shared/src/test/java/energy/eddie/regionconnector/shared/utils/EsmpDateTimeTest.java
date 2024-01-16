package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EsmpDateTimeTest {

    @Test
    void zonedDateTimeToESMPDateTimeString_withUTCOffset_producesExpectedOutput() {
        // Given
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneId.of("Europe/Vienna"));
        EsmpDateTime dateTime = new EsmpDateTime(zonedDateTime);
        String expected = "2023-01-01T01:01Z";

        // When
        String result = dateTime.toString();

        // Then
        assertEquals(expected, result);
    }
}
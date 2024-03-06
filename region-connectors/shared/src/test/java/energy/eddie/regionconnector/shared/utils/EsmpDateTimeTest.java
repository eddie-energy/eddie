package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EsmpDateTimeTest {

    @Test
    void zonedDateTimeToESMPDateTimeString_withUTCOffset_producesExpectedOutput() {
        // Given
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneId.of("Europe/Vienna"));
        EsmpDateTime dateTime = new EsmpDateTime(zonedDateTime);
        String expected = "2023-01-01T00:01:01Z";

        // When
        String result = dateTime.toString();

        // Then
        assertEquals(expected, result);
    }

    @Test
    void zonedDateTimeToESMPDateTimeString_withUTCOffsetAndMinutePrecision_producesExpectedOutput() {
        // Given
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneId.of("Europe/Vienna"));
        EsmpDateTime dateTime = new EsmpDateTime(zonedDateTime, ChronoUnit.MINUTES);
        String expected = "2023-01-01T00:01Z";

        // When
        String result = dateTime.toString();

        // Then
        assertEquals(expected, result);
    }

    @Test
    void esmpDateTime_plusTemporalAmount_producesExpectedOutput() {
        // Given
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        EsmpDateTime dateTime = new EsmpDateTime(zonedDateTime);
        String expected = "2023-01-01T02:01:01Z";

        // When
        String result = dateTime.plus(Duration.of(1, ChronoUnit.HOURS)).toString();

        // Then
        assertEquals(expected, result);
    }

    @Test
    void esmpDateTimeNow_returns() {
        // Given
        // When
        var res = EsmpDateTime.now();

        // Then
        assertNotNull(res);
    }

    @Test
    void esmpDateTimeNow_producesExpectedOutput() {
        // Given
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        Clock clock = Clock.fixed(zonedDateTime.toInstant(), ZoneOffset.UTC);
        String expected = "2023-01-01T01:01:01Z";

        // When
        var res = EsmpDateTime.now(clock).toString();

        // Then
        assertEquals(expected, res);
    }
}
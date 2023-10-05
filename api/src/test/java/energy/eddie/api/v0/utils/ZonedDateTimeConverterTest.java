package energy.eddie.api.v0.utils;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZonedDateTimeConverterTest {

    @Test
    void invoke_parsesDateTime() {
        // Given
        ZonedDateTime dateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, Clock.systemUTC().getZone());
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter();

        // When
        var res = converter.invoke(dateTime.format(DateTimeFormatter.ISO_DATE));

        // Then
        assertEquals(dateTime, res);
    }

    @Test
    void invoke_withNull_returnsNull() {
        // Given
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter();

        // When
        var res = converter.invoke(null);

        // Then
        assertNull(res);
    }

    @Test
    void invoke_withMalformedString_returnsNull() {
        // Given
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter();

        // When
        var res = converter.invoke("not a date string");

        // Then
        assertNull(res);
    }
}

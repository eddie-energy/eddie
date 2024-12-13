package energy.eddie.regionconnector.shared.cim.v0_82;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime.ESMP_DATE_TIME_MINUTE_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EsmpTimeIntervalTest {
    private final DateTimeFormatter format = ESMP_DATE_TIME_MINUTE_FORMATTER;

    @Test
    void testFirstConstructor() {

        // Given
        ZonedDateTime startDateTime = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        ZonedDateTime endDateTime = ZonedDateTime.of(2023, 1, 2, 1, 1, 1, 1, ZoneOffset.UTC);

        // When
        EsmpTimeInterval esmpTimeInterval = new EsmpTimeInterval(startDateTime, endDateTime);

        // Then
        assertEquals("2023-01-01T01:01Z", esmpTimeInterval.start());
        assertEquals("2023-01-02T01:01Z", esmpTimeInterval.end());
    }

    @Test
    void testWithNullValues() {

        // Given
        ZonedDateTime startDateTime = null;
        ZonedDateTime endDateTime = null;

        // When
        EsmpTimeInterval esmpTimeInterval = new EsmpTimeInterval(startDateTime, endDateTime);

        // Then
        assertEquals(Strings.EMPTY, esmpTimeInterval.start());
        assertEquals(Strings.EMPTY, esmpTimeInterval.end());
    }

    @Test
    void testSecondConstructor() {
        // Given
        String start = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).format(format);
        String end = ZonedDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC).format(format);

        // When
        EsmpTimeInterval esmpTimeInterval = new EsmpTimeInterval(start, end, format, ZoneOffset.UTC);

        // Then
        assertEquals(start, esmpTimeInterval.start());
        assertEquals(end, esmpTimeInterval.end());
    }
}
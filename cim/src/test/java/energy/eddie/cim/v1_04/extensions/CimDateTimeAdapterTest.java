package energy.eddie.cim.v1_04.extensions;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CimDateTimeAdapterTest {

    @Test
    void marshal_returnISO8601_withoutMilliseconds() {
        // Given
        var input = ZonedDateTime.of(2025, 1, 1, 0, 0, 10, 12, ZoneOffset.UTC);
        var expected = "2025-01-01T00:00:10Z";
        var adapter = new CimDateTimeAdapter();

        // When
        var res = adapter.marshal(input);

        // Then
        assertEquals(expected, res);
    }

    @Test
    void marshal_onNull_returnsNull() {
        // Given
        var adapter = new CimDateTimeAdapter();

        // When
        var res = adapter.marshal(null);

        // Then
        assertNull(res);
    }

    @Test
    void unmarshal_returns() {
        // Given
        var expected = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var input = "2025-01-01T00:00:00Z";
        var adapter = new CimDateTimeAdapter();

        // When
        var res = adapter.unmarshal(input);

        // Then
        assertEquals(expected, res);
    }

    @Test
    void unmarshal_onNull_returnsNull() {
        // Given
        var adapter = new CimDateTimeAdapter();

        // When
        var res = adapter.unmarshal(null);

        // Then
        assertNull(res);
    }
}
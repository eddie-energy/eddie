package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.shared.utils.LocalDateUtils.endOfDay;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDateUtilsTest {
    @Test
    void testEndOfDay_returnsEndOfDay() {
        // Given
        var date = LocalDate.of(2024, 4, 19);

        // When
        var eod = endOfDay(date, ZoneOffset.UTC);

        // Then
        assertEquals(ZonedDateTime.of(2024, 4, 19, 23, 59, 59, 0, ZoneOffset.UTC), eod);
    }
}
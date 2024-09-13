package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.oldestDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilsTest {
    @Test
    void testEndOfDay_returnsEndOfDay() {
        // Given
        var date = LocalDate.of(2024, 4, 19);

        // When
        var eod = endOfDay(date, ZoneOffset.UTC);

        // Then
        assertEquals(ZonedDateTime.of(2024, 4, 19, 23, 59, 59, 0, ZoneOffset.UTC), eod);
    }

    @Test
    void testLatestDateTime_returnsOldestDateTime() {
        // Given
        var date1 = ZonedDateTime.of(2024, 4, 19, 0, 0, 0, 0, ZoneOffset.UTC);
        var date2 = ZonedDateTime.of(2024, 4, 29, 0, 0, 0, 0, ZoneOffset.UTC);
        var dates = List.of(date1, date2);

        // When
        var res = oldestDateTime(dates);

        // Then
        assertTrue(res.isPresent());
        assertEquals(date1, res.get());
    }

    @Test
    void testOldestDateTime_ofEmptyList_returnsEmptyOptional() {
        // Given
        List<ZonedDateTime> dates = List.of();

        // When
        var res = oldestDateTime(dates);

        // Then
        assertTrue(res.isEmpty());
    }
}
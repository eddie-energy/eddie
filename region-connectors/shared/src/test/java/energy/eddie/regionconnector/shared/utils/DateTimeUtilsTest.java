package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
        var date3 = ZonedDateTime.of(2024, 4, 10, 0, 0, 0, 0, ZoneOffset.UTC);
        var dates = List.of(date1, date2, date3);

        // When
        var res = oldestDateTime(dates);

        // Then
        assertTrue(res.isPresent());
        assertEquals(date3, res.get());
    }

    @Test
    void testLatestDateTime_withOneNull_returnsEmpty() {
        // Given
        var date1 = ZonedDateTime.of(2024, 4, 19, 0, 0, 0, 0, ZoneOffset.UTC);
        var dates = new ArrayList<ZonedDateTime>();
        dates.add(date1);
        dates.add(null);

        // When
        var res = oldestDateTime(dates);

        // Then
        assertTrue(res.isEmpty());
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
// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.*;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testIsBeforeOrEquals_forEqualDates_returnsTrue() {
        // Given
        var date = LocalDate.of(2025, 1, 1);

        // When
        var res = isBeforeOrEquals(date, date);

        // Then
        assertTrue(res);
    }

    @Test
    void testIsBeforeOrEquals_whereLeftIsBeforeRight_returnsTrue() {
        // Given
        var left = LocalDate.of(2025, 1, 1);
        var right = LocalDate.of(2025, 1, 2);

        // When
        var res = isBeforeOrEquals(left, right);

        // Then
        assertTrue(res);
    }

    @Test
    void testIsBeforeOrEquals_whereLeftIsAfterRight_returnsTrue() {
        // Given
        var left = LocalDate.of(2025, 1, 2);
        var right = LocalDate.of(2025, 1, 1);

        // When
        var res = isBeforeOrEquals(left, right);

        // Then
        assertFalse(res);
    }
}
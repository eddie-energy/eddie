// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

public class DateTimeUtils {
    private DateTimeUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime endOfDay(LocalDate localDate, ZoneId zone) {
        return localDate.plusDays(1)
                        .atStartOfDay(zone)
                        .minusSeconds(1);
    }


    /**
     * Finds the oldest date time in a collection of data times.
     * If the collection is empty or only contains null values, returns an empty optional.
     *
     * @param dateTimes the collection of date times
     * @return the oldest datetime or empty optional if the collection is empty, or filled with null values.
     */
    public static Optional<ZonedDateTime> oldestDateTime(Collection<ZonedDateTime> dateTimes) {
        ZonedDateTime oldest = null;
        for (var value : dateTimes) {
            if (value == null) {
                return Optional.empty();
            }
            if (oldest == null || value.isBefore(oldest)) {
                oldest = value;
            }
        }
        return Optional.ofNullable(oldest);
    }

    /**
     * Checks if left is before or equal to right.
     * Read as {@code left <= right}
     *
     * @param left  left-hand operand
     * @param right right-hand operand
     * @return true if left is before or equals to right, otherwise false
     */
    public static boolean isBeforeOrEquals(LocalDate left, LocalDate right) {
        return left.isBefore(right) || left.isEqual(right);
    }
}

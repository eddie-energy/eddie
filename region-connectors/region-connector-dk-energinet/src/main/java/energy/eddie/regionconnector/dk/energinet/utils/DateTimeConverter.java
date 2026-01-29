// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter {
    private DateTimeConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static ZonedDateTime isoDateToZonedDateTime(String isoDate, String zonedId) {
        LocalDateTime localDateTime = LocalDate.parse(isoDate, DateTimeFormatter.ISO_DATE).atStartOfDay();

        return ZonedDateTime.of(localDateTime, ZoneId.of(zonedId));
    }

    public static ZonedDateTime isoDateTimeToZonedDateTime(String isoDateTime, String zonedId) {
        LocalDateTime localDateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME);

        return ZonedDateTime.of(localDateTime, ZoneId.of(zonedId));
    }
}

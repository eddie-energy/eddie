// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

class DateTimeConverterTest {
    private static final String DK_ZONE_ID_STRING = "Europe/Copenhagen";
    private static final ZoneId DK_ZONE_ID = ZoneId.of(DK_ZONE_ID_STRING);

    @Test
    void isoDateToZonedDateTime_asExpected() {
        String isoDate = "2023-06-01";
        ZonedDateTime expected = ZonedDateTime.of(2023, 6, 1, 0, 0, 0, 0, DK_ZONE_ID);

        Assertions.assertEquals(expected, DateTimeConverter.isoDateToZonedDateTime(isoDate, DK_ZONE_ID_STRING));
    }

    @Test
    void isoDateToZonedDateTime_wrongFormat_throws() {
        String isoDate = "01-06-2023";

        Assertions.assertThrows(DateTimeException.class, () -> DateTimeConverter.isoDateToZonedDateTime(isoDate, DK_ZONE_ID_STRING));
    }

    @Test
    void isoDateTimeToZonedDateTime_asExpected() {
        String isoDate = "2023-06-01T22:00:00Z";
        ZonedDateTime expected = ZonedDateTime.of(2023, 6, 1, 22, 0, 0, 0, DK_ZONE_ID);

        Assertions.assertEquals(expected, DateTimeConverter.isoDateTimeToZonedDateTime(isoDate, DK_ZONE_ID_STRING));
    }

    @Test
    void isoDateTimeToZonedDateTime_wrongFormat_throws() {
        String isoDate = "01-06-2023T22:00:00Z";

        Assertions.assertThrows(DateTimeException.class, () -> DateTimeConverter.isoDateTimeToZonedDateTime(isoDate, DK_ZONE_ID_STRING));
    }
}

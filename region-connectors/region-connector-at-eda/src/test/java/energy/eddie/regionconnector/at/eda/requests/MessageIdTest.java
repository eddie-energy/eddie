// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageIdTest {

    @Test
    void routingAddressNull_throws() {
        // given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        // when
        // then
        assertThrows(NullPointerException.class, () -> new MessageId(null, now));
    }

    @Test
    void dateTimeNull_throws() {
        // given
        // when
        // then
        assertThrows(NullPointerException.class, () -> new MessageId("test", null));
    }

    @Test
    void messageIdToString() {
        // given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var messageId = new MessageId("AT999999", now);
        // when
        var result = messageId.toString();
        // then
        var expected = "AT999999T" + now.toInstant().toEpochMilli();
        assertEquals(expected, result);
    }

    @Test
    void messageIdAtMaximumLength_isCreated() {
        // given
        var dateTime = ZonedDateTime.parse("2026-09-01T12:00:00Z");

        // when
        var result = new MessageId("1234567890123EP123456", dateTime).toString();

        // then
        assertEquals(MessageId.MAX_LENGTH, result.length());
    }

    @Test
    void messageIdOverMaximumLength_throws() {
        // given
        var dateTime = ZonedDateTime.parse("2026-09-01T12:00:00Z");

        // when
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MessageId("12345678901234EP123456", dateTime)
        );

        // then
        assertEquals("EDA grouping ID length 36 exceeds the maximum of 35 characters", exception.getMessage());
    }
}

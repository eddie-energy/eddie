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
}

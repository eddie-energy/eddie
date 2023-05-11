package at.eda.requests;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

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
        var address = new RoutingAddress();
        // when
        // then
        assertThrows(NullPointerException.class, () -> new MessageId(address, null));
    }

    @Test
    void messageIdToString() {
        // given
        var address = new RoutingAddress();
        address.setMessageAddress("AT999999");
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var messageId = new MessageId(address, now);
        // when
        var result = messageId.toString();
        // then
        var expected = "AT999999T" + now.toInstant().toEpochMilli();
        assertEquals(expected, result);
    }

}
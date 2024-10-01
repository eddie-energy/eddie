package energy.eddie.aiida.web.webhook.dtos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectionAcknowledgementTest {
    @Test
    void testFromValue_ValidValues() {
        assertEquals(ConnectionAcknowledgement.SUCCESS, ConnectionAcknowledgement.fromValue("success"));
        assertEquals(ConnectionAcknowledgement.FAILURE, ConnectionAcknowledgement.fromValue("failure"));
    }

    @Test
    void testFromValue_InvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ConnectionAcknowledgement.fromValue("invalid_value");
        });
        assertEquals("Invalid connAck value: invalid_value", exception.getMessage());
    }

    @Test
    void testToString() {
        assertEquals("success", ConnectionAcknowledgement.SUCCESS.toString());
        assertEquals("failure", ConnectionAcknowledgement.FAILURE.toString());
    }
}

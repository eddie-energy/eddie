package energy.eddie.aiida.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttConfigTest {
    @Test
    void givenInvalidKeepAlive_throws() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> new MqttConfig("localhost:9092", "subscribeTopic", -10));
        assertEquals("keepAliveInterval needs to be <= 0 seconds", thrown.getMessage());
    }

    @Test
    void givenEitherUsernameOrPasswordNull_throws() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> new MqttConfig("localhost:9092",
                "subscribeTopic", true, true, 60, null, "NotNull"));
        assertEquals("When using authentication, both username and password have to be supplied", thrown.getMessage());

        thrown = assertThrows(IllegalArgumentException.class, () -> new MqttConfig("localhost:9092",
                "subscribeTopic", true, true, 60, "NotNull", null));
        assertEquals("When using authentication, both username and password have to be supplied", thrown.getMessage());
    }

    @Test
    void givenValidInput_doesNotThrow() {
        assertDoesNotThrow(() -> new MqttConfig("localhost:9092", "subscribeTopic",
                true, true, 60, "User", "Pass"));
        assertDoesNotThrow(() -> new MqttConfig("localhost:9092", "subscribeTopic", "user", "pass"));
        assertDoesNotThrow(() -> new MqttConfig("localhost:9092", "subscribeTopic", 5));
        assertDoesNotThrow(() -> new MqttConfig("localhost:9092", "subscribeTopic"));
    }
}
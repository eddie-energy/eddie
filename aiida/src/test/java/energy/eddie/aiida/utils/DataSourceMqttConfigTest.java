package energy.eddie.aiida.utils;

import energy.eddie.aiida.datasources.DataSourceMqttConfig.MqttConfigBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceMqttConfigTest {
    @Test
    void givenInvalidKeepAlive_throws() {
        var builder = new MqttConfigBuilder("localhost:9092", "subscribeTopic");

        var thrown = assertThrows(IllegalArgumentException.class, () -> builder.setKeepAliveInterval(-10));

        assertEquals("keepAliveInterval needs to be <= 0 seconds", thrown.getMessage());
    }

    @Test
    void givenUsernameButNoPassword_throws() {
        var builder = new MqttConfigBuilder("localhost:9092", "subscribeTopic")
                .setUsername("Username")
                .setPassword(null);

        var thrown = assertThrows(IllegalArgumentException.class, builder::build);

        assertEquals("When supplying a username, a password has to be supplied as well", thrown.getMessage());
    }

    @Test
    void givenValidInput_asExpected() {
        var mqttConfig = new MqttConfigBuilder("localhost:9092", "subscribeTopic")
                .setUsername("Username")
                .setPassword("Password")
                .setKeepAliveInterval(40)
                .setAutomaticReconnect(false)
                .setCleanStart(true).build();

        assertEquals("localhost:9092", mqttConfig.serverURI());
        assertEquals("subscribeTopic", mqttConfig.subscribeTopic());
        assertEquals("Username", mqttConfig.username());
        assertEquals("Password", mqttConfig.password());
        assertEquals(40, mqttConfig.keepAliveInterval());
        assertFalse(mqttConfig.automaticReconnect());
        assertTrue(mqttConfig.cleanStart());
    }
}
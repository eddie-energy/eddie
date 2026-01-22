package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiidaBeanConfigTest {
    @Mock
    private AiidaConfiguration aiidaConfiguration;

    private AiidaBeanConfig aiidaBeanConfig;

    @BeforeEach
    void setUp() {
        aiidaBeanConfig = new AiidaBeanConfig();
    }

    @Test
    void connectionOptions_shouldCreateMqttConnectionOptions() {
        // Given
        var mqttUsername = "testUser";
        var mqttPassword = "testPassword";
        when(aiidaConfiguration.mqttUsername()).thenReturn(mqttUsername);
        when(aiidaConfiguration.mqttPassword()).thenReturn(mqttPassword);

        // When
        var connectionOptions = aiidaBeanConfig.connectionOptions(aiidaConfiguration);

        // Then
        assertEquals(mqttUsername, connectionOptions.getUserName());
        assertArrayEquals(mqttPassword.getBytes(StandardCharsets.UTF_8), connectionOptions.getPassword());
    }

    @Test
    void connectionOptions_shouldSkipPassword_whenEmpty(){
        // Given
        var mqttUsername = "testUser";
        var mqttPassword = "  ";

        when(aiidaConfiguration.mqttUsername()).thenReturn(mqttUsername);
        when(aiidaConfiguration.mqttPassword()).thenReturn(mqttPassword);

        // When
        var connectionOptions = aiidaBeanConfig.connectionOptions(aiidaConfiguration);

        // Then
        assertEquals(mqttUsername, connectionOptions.getUserName());
        assertNull(connectionOptions.getPassword());
    }
}

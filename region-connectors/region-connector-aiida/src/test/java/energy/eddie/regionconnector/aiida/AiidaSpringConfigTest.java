package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiidaSpringConfigTest {
    @Test
    void givenLocalhost_aiidaConfiguration_constructsHandshakeUrlTemplateAsExpected() {
        // When
        AiidaConfiguration config = new AiidaBeanConfig().aiidaConfiguration("foo",
                                                                               5,
                                                                               "http://localhost:1234",
                                                                               "fooBar",
                                                                               null);

        // Then
        assertEquals("foo", config.customerId());
        assertEquals(5, config.bCryptStrength());
        assertEquals("http://localhost:1234/region-connectors/aiida/permission-request/{permissionId}",
                     config.handshakeUrl());
    }

    @Test
    void givenEmpty_aiidaConfiguration_usesNull() {
        // When
        AiidaConfiguration config = new AiidaBeanConfig().aiidaConfiguration("foo",
                                                                               5,
                                                                               "http://localhost:1234",
                                                                               "fooBar",
                                                                               "   ");

        // Then
        assertNull(config.mqttPassword());
    }
}

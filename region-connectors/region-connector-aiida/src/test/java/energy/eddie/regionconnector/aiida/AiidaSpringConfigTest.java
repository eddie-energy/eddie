package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiidaSpringConfigTest {
    @Test
    void givenLocalhost_aiidaConfiguration_constructsHandshakeUrlTemplateAsExpected() {
        // When
        AiidaConfiguration config = new AiidaSpringConfig().aiidaConfiguration("foo", 5, "http://localhost:1234");

        // Then
        assertEquals("foo", config.customerId());
        assertEquals(5, config.bCryptStrength());
        assertEquals("http://localhost:1234/region-connectors/aiida/permission-request/{permissionId}",
                     config.handshakeUrl());
    }
}

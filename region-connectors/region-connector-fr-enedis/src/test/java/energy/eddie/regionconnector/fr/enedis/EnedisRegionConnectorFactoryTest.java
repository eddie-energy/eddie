package energy.eddie.regionconnector.fr.enedis;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class EnedisRegionConnectorFactoryTest {
    @Test
    void create_withValidConfig_returnsRegionConnector() {
        // Given
        var config = mock(Config.class);
        EnedisRegionConnectorFactory uut = new EnedisRegionConnectorFactory();

        // When
        var connector = uut.create(config);

        // Then
        assertNotNull(connector);
    }

}
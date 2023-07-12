package energy.eddie.regionconnector.fr.enedis;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EnedisRegionConnectorFactoryTest {
    @Test
    void create_withNullConfig_throwsException() {
        // Arrange
        EnedisRegionConnectorFactory sut = new EnedisRegionConnectorFactory();

        assertThrows(NullPointerException.class, () -> {
            // Act
            sut.create(null);
        });
    }

    @Test
    void create_withValidConfig_returnsRegionConnector() throws Exception {
        // Arrange
        EnedisRegionConnectorFactory sut = new EnedisRegionConnectorFactory();

        var config = mock(Config.class);

        // Act
        var connector = sut.create(config);

        assertNotNull(connector);
    }

}
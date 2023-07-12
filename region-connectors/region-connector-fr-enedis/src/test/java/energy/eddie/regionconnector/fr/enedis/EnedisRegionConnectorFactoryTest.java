package energy.eddie.regionconnector.fr.enedis;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EnedisRegionConnectorFactoryTest {
    @Test
    void create_withNullConfig_throwsException() {
        EnedisRegionConnectorFactory uut = new EnedisRegionConnectorFactory();

        assertThrows(NullPointerException.class, () -> uut.create(null));
    }

    @Test
    void create_withValidConfig_returnsRegionConnector() throws Exception {
        var config = mock(Config.class);

        EnedisRegionConnectorFactory uut = new EnedisRegionConnectorFactory();
        var connector = uut.create(config);

        assertNotNull(connector);
    }

}
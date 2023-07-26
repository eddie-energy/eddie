package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnectorProvisioningException;
import energy.eddie.regionconnector.fr.enedis.config.ConfigEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisRegionConnectorFactoryTest {
    @Test
    void create_withNullConfig_throwsException() {
        EnedisRegionConnectorFactory uut = new EnedisRegionConnectorFactory();

        assertThrows(NullPointerException.class, () -> uut.create(null));
    }

    @Test
    void create_withInvalidEnedisConfig_throwsRegionConnectorProvisioningException() {
        EnedisRegionConnectorFactory uut = new EnedisRegionConnectorFactory();

        var config = mock(Config.class);
        when(config.getValue(EnedisConfiguration.ENEDIS_CLIENT_ID_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(EnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(EnedisConfiguration.ENEDIS_BASE_PATH_KEY, String.class)).thenThrow(NoSuchElementException.class);

        assertThrows(RegionConnectorProvisioningException.class, () -> uut.create(config));
    }

    @Test
    void create_withValidConfig_returnsRegionConnector() throws Exception {
        var config = mock(Config.class);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, String.class)).thenReturn("id");
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, String.class)).thenReturn("secret");
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_BASE_PATH_KEY, String.class)).thenReturn("path");

        EnedisRegionConnectorFactory uut = new EnedisRegionConnectorFactory();
        var connector = uut.create(config);

        assertNotNull(connector);
    }

}
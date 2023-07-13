package energy.eddie.regionconnector.fr.enedis.config;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigEnedisConfigurationTest {
    @Test
    void configurationThrows_ifConfigNull() {
        assertThrows(NullPointerException.class, () -> new ConfigEnedisConfiguration(null));
    }

    @Test
    void configurationConstructs() {
        var config = mock(Config.class);

        var uut = new ConfigEnedisConfiguration(config);

        assertNotNull(uut);
    }

    @Test
    void getters_withValidConfig_returnExpected() {
        var config = mock(Config.class);
        var expectedId = "id";
        var expectedSecret = "secret";
        var expectedPath = "path";

        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, String.class)).thenReturn(expectedId);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, String.class)).thenReturn(expectedSecret);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_BASE_PATH_KEY, String.class)).thenReturn(expectedPath);

        var uut = new ConfigEnedisConfiguration(config);

        assertEquals(expectedId, uut.clientId());
        assertEquals(expectedSecret, uut.clientSecret());
        assertEquals(expectedPath, uut.basePath());
    }

    @Test
    void getters_withMissingValues_throwsNoSuchElementException() {
        var config = mock(Config.class);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_BASE_PATH_KEY, String.class)).thenThrow(NoSuchElementException.class);

        var uut = new ConfigEnedisConfiguration(config);
        assertThrows(NoSuchElementException.class, uut::clientId);
        assertThrows(NoSuchElementException.class, uut::clientSecret);
        assertThrows(NoSuchElementException.class, uut::basePath);
    }

    @Test
    void getters_withValidConfig_returnsUpdated() {
        var config = mock(Config.class);
        var expectedId = "id";
        var updatedId = "updatedId";
        var expectedSecret = "secret";
        var updatedSecret = "updatedSecret";
        var expectedPath = "path";
        var updatedPath = "updatedPath";


        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, String.class))
                .thenReturn(expectedId)
                .thenReturn(updatedId);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, String.class))
                .thenReturn(expectedSecret)
                .thenReturn(updatedSecret);
        when(config.getValue(ConfigEnedisConfiguration.ENEDIS_BASE_PATH_KEY, String.class))
                .thenReturn(expectedPath)
                .thenReturn(updatedPath);


        var uut = new ConfigEnedisConfiguration(config);

        // first values
        assertAll(
                () -> assertEquals(expectedId, uut.clientId()),
                () -> assertEquals(expectedSecret, uut.clientSecret()),
                () -> assertEquals(expectedPath, uut.basePath())
        );

        // updated values
        assertAll(
                () -> assertEquals(updatedId, uut.clientId()),
                () -> assertEquals(updatedSecret, uut.clientSecret()),
                () -> assertEquals(updatedPath, uut.basePath())
        );
    }
}
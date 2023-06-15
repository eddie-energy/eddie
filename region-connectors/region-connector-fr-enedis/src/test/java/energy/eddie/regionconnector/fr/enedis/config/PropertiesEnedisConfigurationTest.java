package energy.eddie.regionconnector.fr.enedis.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesEnedisConfigurationTest {
    @Test
    void getClientId() {
        // given
        Properties props = new Properties();
        props.put(PropertiesEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, "clientId");
        props.put(PropertiesEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, "clientSecret");
        String expected = "clientId";
        PropertiesEnedisConfiguration enedisConfiguration = new PropertiesEnedisConfiguration(props);

        // when
        var actual = enedisConfiguration.clientId();

        // then
        assertEquals(expected, actual);
    }

    @Test
    void getClientSecret() {
        // given
        Properties props = new Properties();
        props.put(PropertiesEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, "clientId");
        props.put(PropertiesEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, "clientSecret");
        String expected = "clientSecret";
        PropertiesEnedisConfiguration enedisConfiguration = new PropertiesEnedisConfiguration(props);

        // when
        var actual = enedisConfiguration.clientSecret();

        // then
        assertEquals(expected, actual);
    }

    @Test
    void basePathHasDefaultValue() {
        // given
        Properties props = new Properties();
        props.put(PropertiesEnedisConfiguration.ENEDIS_CLIENT_ID_KEY, "clientId");
        props.put(PropertiesEnedisConfiguration.ENEDIS_CLIENT_SECRET_KEY, "clientSecret");
        String expected = "https://ext.prod.api.enedis.fr";
        PropertiesEnedisConfiguration enedisConfiguration = new PropertiesEnedisConfiguration(props);

        // when
        var actual = enedisConfiguration.basePath();

        // then
        assertEquals(expected, actual);
    }

    @Test
    void constructorThrowsWhenRequiredPropertiesNotSet() {
        // given
        Properties props = new Properties();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new PropertiesEnedisConfiguration(props));
    }

    @Test
    void constructorThrowsWhenPropertiesNull() {
        // given
        // when
        // then
        assertThrows(NullPointerException.class, () -> new PropertiesEnedisConfiguration(null));
    }
}
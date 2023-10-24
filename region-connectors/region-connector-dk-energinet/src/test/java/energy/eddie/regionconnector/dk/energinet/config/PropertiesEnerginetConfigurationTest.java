package energy.eddie.regionconnector.dk.energinet.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesEnerginetConfigurationTest {
    @Test
    void getCustomerBasePath() {
        // given
        Properties props = new Properties();
        props.put(PropertiesEnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, "customerPath");
        props.put(PropertiesEnerginetConfiguration.ENERGINET_THIRDPARTY_BASE_PATH_KEY, "thirdPartyPath");
        String expected = "customerPath";
        PropertiesEnerginetConfiguration enedisConfiguration = new PropertiesEnerginetConfiguration(props);

        // when
        var actual = enedisConfiguration.customerBasePath();

        // then
        assertEquals(expected, actual);
    }

    @Test
    void getThirdpartyBasePath() {
        // given
        Properties props = new Properties();
        props.put(PropertiesEnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, "customerPath");
        props.put(PropertiesEnerginetConfiguration.ENERGINET_THIRDPARTY_BASE_PATH_KEY, "thirdPartyPath");
        String expected = "thirdPartyPath";
        PropertiesEnerginetConfiguration enedisConfiguration = new PropertiesEnerginetConfiguration(props);

        // when
        var actual = enedisConfiguration.thirdpartyBasePath();

        // then
        assertEquals(expected, actual);
    }

    @Test
    void constructorThrowsWhenRequiredPropertiesNotSet() {
        // given
        Properties props = new Properties();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new PropertiesEnerginetConfiguration(props));
    }

    @Test
    void constructorThrowsWhenPropertiesNull() {
        // given
        // when
        // then
        assertThrows(NullPointerException.class, () -> new PropertiesEnerginetConfiguration(null));
    }
}
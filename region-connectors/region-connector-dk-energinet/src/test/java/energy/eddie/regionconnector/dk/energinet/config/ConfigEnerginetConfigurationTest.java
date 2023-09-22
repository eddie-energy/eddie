package energy.eddie.regionconnector.dk.energinet.config;

import energy.eddie.regionconnector.dk.energinet.config.ConfigEnerginetConfiguration;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigEnerginetConfigurationTest {
    @Test
    void configurationThrows_ifConfigNull() {
        assertThrows(NullPointerException.class, () -> new ConfigEnerginetConfiguration(null));
    }

    @Test
    void configurationConstructs() {
        var config = mock(Config.class);

        var uut = new ConfigEnerginetConfiguration(config);

        assertNotNull(uut);
    }

    @Test
    void getters_withValidConfig_returnExpected() {
        var config = mock(Config.class);
        var expectedCustomerBasePath = "customerPath";
        var expectedThirdPartyBasePath = "thirdPartyPath";

        when(config.getValue(ConfigEnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, String.class)).thenReturn(expectedCustomerBasePath);
        when(config.getValue(ConfigEnerginetConfiguration.ENERGINET_THIRDPARTY_BASE_PATH_KEY, String.class)).thenReturn(expectedThirdPartyBasePath);

        var uut = new ConfigEnerginetConfiguration(config);

        assertEquals(expectedCustomerBasePath, uut.customerBasePath());
        assertEquals(expectedThirdPartyBasePath, uut.thirdpartyBasePath());
    }

    @Test
    void getters_withMissingValues_throwsNoSuchElementException() {
        var config = mock(Config.class);
        when(config.getValue(ConfigEnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(ConfigEnerginetConfiguration.ENERGINET_THIRDPARTY_BASE_PATH_KEY, String.class)).thenThrow(NoSuchElementException.class);

        var uut = new ConfigEnerginetConfiguration(config);
        assertThrows(NoSuchElementException.class, uut::customerBasePath);
        assertThrows(NoSuchElementException.class, uut::thirdpartyBasePath);
    }

    @Test
    void getters_withValidConfig_returnsUpdated() {
        var config = mock(Config.class);
        var expectedCustomerBasePath = "cpath";
        var updatedCustomerBasePath = "customerPath";
        var expectedThirdPartyBasePath = "tpath";
        var updatedThirdPartyBasePath = "thirdPartyPath";

        when(config.getValue(ConfigEnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, String.class))
                .thenReturn(expectedCustomerBasePath)
                .thenReturn(updatedCustomerBasePath);
        when(config.getValue(ConfigEnerginetConfiguration.ENERGINET_THIRDPARTY_BASE_PATH_KEY, String.class))
                .thenReturn(expectedThirdPartyBasePath)
                .thenReturn(updatedThirdPartyBasePath);


        var uut = new ConfigEnerginetConfiguration(config);

        // first values
        assertAll(
                () -> assertEquals(expectedCustomerBasePath, uut.customerBasePath()),
                () -> assertEquals(expectedThirdPartyBasePath, uut.thirdpartyBasePath())
        );

        // updated values
        assertAll(
                () -> assertEquals(updatedCustomerBasePath, uut.customerBasePath()),
                () -> assertEquals(updatedThirdPartyBasePath, uut.thirdpartyBasePath())
        );
    }
}
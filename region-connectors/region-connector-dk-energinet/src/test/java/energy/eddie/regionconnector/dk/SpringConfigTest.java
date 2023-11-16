package energy.eddie.regionconnector.dk;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringConfigTest {

    @Test
    void springConfig_createsEnerginetConfiguration() {
        // Given
        Config config = mock(Config.class);
        SpringConfig springConfig = new SpringConfig();

        // When
        EnerginetConfiguration energinetConfiguration = springConfig.energinetConfiguration(config);

        // Then
        assertNotNull(energinetConfiguration);
    }

    @Test
    void springConfig_createsEnerginetCustomerApi() {
        // Given
        Config config = mock(Config.class);
        SpringConfig springConfig = new SpringConfig();
        when(config.getValue(EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, String.class)).thenReturn("basePath");

        // When
        EnerginetCustomerApi energinetCustomerApi = springConfig.energinetCustomerApi(config);

        // Then
        assertNotNull(energinetCustomerApi);
    }

    @Test
    void springConfig_createsPermissionRequestRepository() {
        // Given
        SpringConfig springConfig = new SpringConfig();

        // When
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = springConfig.permissionRequestRepository();

        // Then
        assertNotNull(permissionRequestRepository);
    }

    @Test
    void springConfig_createsRegionConnector() {
        // Given
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        EnerginetCustomerApi energinetCustomerApi = mock(EnerginetCustomerApi.class);
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();

        SpringConfig springConfig = new SpringConfig();

        // When
        RegionConnector regionConnector = springConfig.regionConnector(configuration, energinetCustomerApi, permissionRequestRepository);

        // Then
        assertNotNull(regionConnector);
    }

    @Test
    void springConfig_createsContainerAndReturnsRegionConnector() throws Exception {
        // Given
        Config config = mock(Config.class);
        when(config.getValue(EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY, String.class)).thenReturn("basePath");

        // When
        RegionConnector regionConnector = SpringConfig.start(config);

        // Then
        assertNotNull(regionConnector);

        // Clean Up
        regionConnector.close();
    }
}

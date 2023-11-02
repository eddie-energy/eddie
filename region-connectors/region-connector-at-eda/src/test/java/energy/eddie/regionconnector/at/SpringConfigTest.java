package energy.eddie.regionconnector.at;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringConfigTest {

    @Test
    void springConfig_createsPontonXPAdapterConfiguration() {
        // Given
        Config config = mock(Config.class);
        SpringConfig springConfig = new SpringConfig();

        // When
        PontonXPAdapterConfiguration xpAdapterConfiguration = springConfig.pontonXPAdapterConfiguration(config);

        // Then
        assertNotNull(xpAdapterConfiguration);
    }

    @Test
    void springConfig_createsAtConfiguration() {
        // Given
        Config config = mock(Config.class);
        SpringConfig springConfig = new SpringConfig();

        // When
        AtConfiguration atConfiguration = springConfig.atConfiguration(config);

        // Then
        assertNotNull(atConfiguration);
    }

    @Test
    void springConfig_createsPermissionRequestRepository() {
        // Given
        SpringConfig springConfig = new SpringConfig();

        // When
        AtPermissionRequestRepository permissionRequestRepository = springConfig.permissionRequestRepository();

        // Then
        assertNotNull(permissionRequestRepository);
    }

    @Test
    void springConfig_createsRegionConnector() throws TransmissionException {
        // Given
        AtConfiguration configuration = mock(AtConfiguration.class);
        AtPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getCMRequestStatusStream())
                .thenReturn(Flux.empty());
        when(edaAdapter.getConsumptionRecordStream())
                .thenReturn(Flux.empty());

        SpringConfig springConfig = new SpringConfig();

        // When
        RegionConnector regionConnector = springConfig.regionConnector(configuration, permissionRequestRepository, edaAdapter);

        // Then
        assertNotNull(regionConnector);
    }

    @Test
    @Disabled("Only works with a running ponton xp messenger instance")
    void springConfig_createsContainerAndReturnsRegionConnector() throws Exception {
        // Given
        Config config = mock(Config.class);
        when(config.getValue(PontonXPAdapterConfiguration.WORK_FOLDER_KEY, String.class))
                .thenReturn("./");
        when(config.getValue(PontonXPAdapterConfiguration.HOSTNAME_KEY, String.class))
                .thenReturn("hostname");

        // When
        RegionConnector regionConnector = SpringConfig.start(config);

        // Then
        assertNotNull(regionConnector);

        // Clean Up
        regionConnector.close();
    }

}
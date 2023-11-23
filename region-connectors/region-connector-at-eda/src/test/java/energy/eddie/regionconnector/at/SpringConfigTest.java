package energy.eddie.regionconnector.at;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringConfigTest {

    @Test
    void springConfig_createsPontonXPAdapterConfiguration() {
        // Given
        SpringConfig springConfig = new SpringConfig();

        // When
        PontonXPAdapterConfiguration xpAdapterConfiguration = springConfig.pontonXPAdapterConfiguration("adapterId", "0.0.0", "localhost", 9200, "/ponton");

        // Then
        assertNotNull(xpAdapterConfiguration);
    }

    @Test
    void springConfig_createsAtConfiguration() {
        // Given
        SpringConfig springConfig = new SpringConfig();

        // When
        AtConfiguration atConfiguration = springConfig.atConfiguration("AT00001");

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
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getCMRequestStatusStream())
                .thenReturn(Flux.empty());
        when(edaAdapter.getConsumptionRecordStream())
                .thenReturn(Flux.empty());

        SpringConfig springConfig = new SpringConfig();
        Sinks.Many<ConnectionStatusMessage> messages = Sinks.many().multicast().onBackpressureBuffer();


        // When
        RegionConnector regionConnector = springConfig.regionConnector(permissionRequestService, edaAdapter, messages, () -> 0);

        // Then
        assertNotNull(regionConnector);
    }

    @Test
    void springConfig_createsContainerAndReturnsRegionConnector() throws Exception {
        // Given
        // When
        RegionConnector regionConnector = SpringConfig.start();

        // Then
        assertNotNull(regionConnector);

        // Clean Up
        regionConnector.close();
    }

}
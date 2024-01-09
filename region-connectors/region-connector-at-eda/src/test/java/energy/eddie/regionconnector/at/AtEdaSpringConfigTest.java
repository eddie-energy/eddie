package energy.eddie.regionconnector.at;

import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AtEdaSpringConfigTest {

    @Test
    void springConfig_createsPontonXPAdapterConfiguration() {
        // Given
        AtEdaSpringConfig springConfig = new AtEdaSpringConfig();

        // When
        PontonXPAdapterConfiguration xpAdapterConfiguration = springConfig.pontonXPAdapterConfiguration("adapterId", "0.0.0", "localhost", 9200, "/ponton");

        // Then
        assertNotNull(xpAdapterConfiguration);
    }

    @Test
    void springConfig_createsAtConfiguration() {
        // Given
        AtEdaSpringConfig springConfig = new AtEdaSpringConfig();

        // When
        AtConfiguration atConfiguration = springConfig.atConfiguration("AT00001", null);

        // Then
        assertNotNull(atConfiguration);
    }

    @Test
    void springConfig_createsPermissionRequestRepository() {
        // Given
        AtEdaSpringConfig springConfig = new AtEdaSpringConfig();

        // When
        AtPermissionRequestRepository permissionRequestRepository = springConfig.permissionRequestRepository();

        // Then
        assertNotNull(permissionRequestRepository);
    }
}
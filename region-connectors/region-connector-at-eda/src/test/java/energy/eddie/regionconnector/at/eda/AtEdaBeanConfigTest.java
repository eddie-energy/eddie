package energy.eddie.regionconnector.at.eda;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AtEdaBeanConfigTest {

    @Test
    void springConfig_createsPontonXPAdapterConfiguration() {
        // Given
        AtEdaBeanConfig springConfig = new AtEdaBeanConfig();

        // When
        PontonXPAdapterConfiguration xpAdapterConfiguration = springConfig.pontonXPAdapterConfiguration(
                "adapterId",
                "0.0.0",
                "localhost",
                9200,
                "localhost/api",
                "/ponton",
                "username",
                "password"
        );

        // Then
        assertNotNull(xpAdapterConfiguration);
    }

    @Test
    void springConfig_createsAtConfiguration() {
        // Given
        AtEdaBeanConfig springConfig = new AtEdaBeanConfig();

        // When
        AtConfiguration atConfiguration = springConfig.atConfiguration("AT00001");

        // Then
        assertNotNull(atConfiguration);
    }
}

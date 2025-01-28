package energy.eddie.regionconnector.us.green.button.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenButtonConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"id", "ID", "Id"})
    void testClientId_ignoresCase(String clientId) {
        // Given
        var config = getGreenButtonConfiguration();

        // When
        var res = config.clientIds().containsKey(clientId);

        // Then
        assertTrue(res);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "ID", "Id"})
    void testClientSecret_ignoresCase(String clientId) {
        // Given
        var config = getGreenButtonConfiguration();

        // When
        var res = config.clientSecrets().containsKey(clientId);

        // Then
        assertTrue(res);
    }

    private static GreenButtonConfiguration getGreenButtonConfiguration() {
        return new GreenButtonConfiguration(
                "http://localhost",
                Map.of("id", "value"),
                Map.of("id", "secret"),
                Map.of("id", "token"),
                "http://localhost",
                "secret");
    }
}
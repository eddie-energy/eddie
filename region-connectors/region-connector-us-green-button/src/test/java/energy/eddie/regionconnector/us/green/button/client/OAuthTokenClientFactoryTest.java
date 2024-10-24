package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthTokenClientFactoryTest {
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "token",
            "http://localhost",
            Map.of("company", "client-id", "only-id", "client-id"),
            Map.of("company", "client-secret", "only-secret", "client-secret"),
            "http://localhost",
            GreenButtonApi.MAX_METER_RESULTS,
            "secret");
    private final OAuthTokenClientFactory factory = new OAuthTokenClientFactory(config);

    @Test
    void create_withMissingClientSecret_throwsException() {
        // Given
        // When & Then
        assertThrows(MissingClientSecretException.class, () -> factory.create("only-id", "http://localhost"));
    }

    @Test
    void create_withMissingClientId_throwsException() {
        // Given
        // When & Then
        assertThrows(MissingClientIdException.class, () -> factory.create("only-secret", "http://localhost"));
    }

    @Test
    void create_withValidCredentials_returnsClient() throws MissingClientIdException, MissingClientSecretException {
        // Given
        // When
        var res = factory.create("company", "http://localhost");

        // Then
        assertNotNull(res);
    }
}
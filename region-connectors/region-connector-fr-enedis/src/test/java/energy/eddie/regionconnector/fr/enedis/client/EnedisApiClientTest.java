package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisApiClientTest {

    @Test
    void healthNotImplemented() {
        // Given
        EnedisConfiguration config = mock(EnedisConfiguration.class);
        when(config.basePath()).thenReturn("https://example.com/");
        EnedisApiClient enedisApiClient = new EnedisApiClient(config);

        // When
        // Then
        assertThrows(IllegalStateException.class, enedisApiClient::health);
    }

}
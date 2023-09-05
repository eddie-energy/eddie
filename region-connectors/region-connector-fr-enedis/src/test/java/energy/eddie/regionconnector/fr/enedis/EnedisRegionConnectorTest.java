package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisRegionConnectorTest {
    @Test
    void health_returnsHealthChecks() {
        // Given
        var config = mock(EnedisConfiguration.class);
        when(config.clientId()).thenReturn("id");
        when(config.clientSecret()).thenReturn("secret");
        when(config.basePath()).thenReturn("path");
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        var rc = new EnedisRegionConnector(config, enedisApi);

        // When
        var res = rc.health();

        // Then
        assertEquals(Map.of("service", HealthState.UP), res);
    }

}
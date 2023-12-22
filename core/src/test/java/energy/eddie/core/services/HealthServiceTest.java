package energy.eddie.core.services;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTest {
    @Test
    void givenNewRegionConnector_returnsHealthsFromAllRegionConnectors() {
        // Given
        var service = new HealthService();

        // When empty Then
        assertEquals(0, service.getRegionConnectorHealth().size());

        // When new service added
        service.registerRegionConnector(createNewTestRegionConnector());

        // Then
        assertEquals(1, service.getRegionConnectorHealth().size());

        // When new service added
        service.registerRegionConnector(createNewTestRegionConnector());

        // Then
        assertEquals(2, service.getRegionConnectorHealth().size());
    }

    public static RegionConnector createNewTestRegionConnector() {
        final int counter = new Random().nextInt();

        Map<String, HealthState> health = new HashMap<>();
        health.put(Integer.toString(counter), HealthState.UP);
        return new RegionConnector() {
            @Override
            public RegionConnectorMetadata getMetadata() {
                var mock = mock(RegionConnectorMetadata.class);
                when(mock.id()).thenReturn(Integer.toString(counter));
                return mock;
            }

            @Override
            public void terminatePermission(String permissionId) {

            }

            @Override
            public Map<String, HealthState> health() {
                return health;
            }
        };
    }
}

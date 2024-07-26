package energy.eddie.core.services;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetadataServiceTest {
    @Test
    void givenNewRegionConnector_returnsMetadataFromAllRegionConnectors() {
        // Given
        var service = new MetadataService();

        // When empty Then
        assertEquals(0, service.getRegionConnectorMetadata().size());

        // When new service added
        service.registerRegionConnector(createNewTestRegionConnector());

        // Then
        assertEquals(1, service.getRegionConnectorMetadata().size());

        // When new service added
        service.registerRegionConnector(createNewTestRegionConnector());

        // Then
        assertEquals(2, service.getRegionConnectorMetadata().size());
    }

    public static RegionConnector createNewTestRegionConnector() {
        final int counter = new Random().nextInt();

        return new RegionConnector() {
            @Override
            public RegionConnectorMetadata getMetadata() {
                var mock = mock(RegionConnectorMetadata.class);
                when(mock.id()).thenReturn(Integer.toString(counter));
                return mock;
            }

            @Override
            public void terminatePermission(String permissionId) {
                // No-Op
            }
        };
    }
}

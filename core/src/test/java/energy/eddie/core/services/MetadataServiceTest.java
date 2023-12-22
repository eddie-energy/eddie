package energy.eddie.core.services;

import org.junit.jupiter.api.Test;

import static energy.eddie.core.services.HealthServiceTest.createNewTestRegionConnector;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

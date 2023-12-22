package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.MetadataService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MetadataServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(MetadataService.class);
        var mockRegionConnector = mock(RegionConnector.class);

        // When, Then
        assertThrows(NullPointerException.class, () -> new MetadataServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class, () -> new MetadataServiceRegistrar(mockRegionConnector, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var mockService = mock(MetadataService.class);
        var mockRegionConnector = mock(RegionConnector.class);

        // When
        new MetadataServiceRegistrar(mockRegionConnector, mockService);

        // Then
        verify(mockService).registerRegionConnector(mockRegionConnector);
    }
}

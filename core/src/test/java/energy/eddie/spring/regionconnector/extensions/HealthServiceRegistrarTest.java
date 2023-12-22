package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.HealthService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HealthServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(HealthService.class);
        var mockRegionConnector = mock(RegionConnector.class);

        // When, Then
        assertThrows(NullPointerException.class, () -> new HealthServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class, () -> new HealthServiceRegistrar(mockRegionConnector, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var mockService = mock(HealthService.class);
        var mockRegionConnector = mock(RegionConnector.class);

        // When
        new HealthServiceRegistrar(mockRegionConnector, mockService);

        // Then
        verify(mockService).registerRegionConnector(mockRegionConnector);
    }
}

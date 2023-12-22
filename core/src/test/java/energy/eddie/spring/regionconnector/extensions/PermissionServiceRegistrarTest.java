package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.core.services.PermissionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PermissionServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(PermissionService.class);
        var mockProvider = mock(Mvp1ConnectionStatusMessageProvider.class);

        // When, Then
        assertThrows(NullPointerException.class, () -> new PermissionServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class, () -> new PermissionServiceRegistrar(mockProvider, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var mockService = mock(PermissionService.class);
        var mockProvider = mock(Mvp1ConnectionStatusMessageProvider.class);

        // When
        new PermissionServiceRegistrar(mockProvider, mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}

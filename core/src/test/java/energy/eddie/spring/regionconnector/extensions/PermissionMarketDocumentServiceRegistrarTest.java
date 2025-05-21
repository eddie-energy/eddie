package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.core.services.v0_82.PermissionMarketDocumentService;
import energy.eddie.spring.regionconnector.extensions.v0_82.PermissionMarketDocumentServiceRegistrar;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PermissionMarketDocumentServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(PermissionMarketDocumentService.class);
        Optional<PermissionMarketDocumentProvider> mockProvider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new PermissionMarketDocumentServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class,
                     () -> new PermissionMarketDocumentServiceRegistrar(mockProvider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(PermissionMarketDocumentService.class);

        // When
        new PermissionMarketDocumentServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(PermissionMarketDocumentService.class);
        var mockProvider = mock(PermissionMarketDocumentProvider.class);

        // When
        new PermissionMarketDocumentServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}
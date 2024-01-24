package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.core.services.ConsentMarketDocumentService;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ConsentMarketDocumentServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(ConsentMarketDocumentService.class);
        Optional<ConsentMarketDocumentProvider> mockProvider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new ConsentMarketDocumentServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class, () -> new ConsentMarketDocumentServiceRegistrar(mockProvider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(ConsentMarketDocumentService.class);

        // When
        new ConsentMarketDocumentServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(ConsentMarketDocumentService.class);
        var mockProvider = mock(CommonConsentMarketDocumentProvider.class);

        // When
        new ConsentMarketDocumentServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}
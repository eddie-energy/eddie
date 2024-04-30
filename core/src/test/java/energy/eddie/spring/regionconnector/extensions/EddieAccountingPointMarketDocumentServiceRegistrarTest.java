package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.core.services.EddieAccountingPointMarketDocumentService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EddieAccountingPointMarketDocumentServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(EddieAccountingPointMarketDocumentService.class);
        Optional<EddieAccountingPointMarketDocumentProvider> provider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new EddieAccountingPointMarketDocumentServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class,
                     () -> new EddieAccountingPointMarketDocumentServiceRegistrar(provider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(EddieAccountingPointMarketDocumentService.class);

        // When
        new EddieAccountingPointMarketDocumentServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(EddieAccountingPointMarketDocumentService.class);
        var mockProvider = mock(EddieAccountingPointMarketDocumentProvider.class);

        // When
        new EddieAccountingPointMarketDocumentServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}

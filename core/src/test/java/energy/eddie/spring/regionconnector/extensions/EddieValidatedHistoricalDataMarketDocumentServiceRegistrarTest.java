package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.core.services.EddieValidatedHistoricalDataMarketDocumentService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EddieValidatedHistoricalDataMarketDocumentServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(EddieValidatedHistoricalDataMarketDocumentService.class);
        Optional<EddieValidatedHistoricalDataMarketDocumentProvider> provider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new EddieValidatedHistoricalDataMarketDocumentServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class, () -> new EddieValidatedHistoricalDataMarketDocumentServiceRegistrar(provider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(EddieValidatedHistoricalDataMarketDocumentService.class);

        // When
        new EddieValidatedHistoricalDataMarketDocumentServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(EddieValidatedHistoricalDataMarketDocumentService.class);
        var mockProvider = mock(EddieValidatedHistoricalDataMarketDocumentProvider.class);

        // When
        new EddieValidatedHistoricalDataMarketDocumentServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}
package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.core.services.ValidatedHistoricalDataEnvelopeService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ValidatedHistoricalDataEnvelopeServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(ValidatedHistoricalDataEnvelopeService.class);
        Optional<ValidatedHistoricalDataEnvelopeProvider> provider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataEnvelopeServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataEnvelopeServiceRegistrar(provider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(ValidatedHistoricalDataEnvelopeService.class);

        // When
        new ValidatedHistoricalDataEnvelopeServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(ValidatedHistoricalDataEnvelopeService.class);
        var mockProvider = mock(ValidatedHistoricalDataEnvelopeProvider.class);

        // When
        new ValidatedHistoricalDataEnvelopeServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}
package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeProvider;
import energy.eddie.core.services.ValidatedHistoricalDataEnveloppeService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ValidatedHistoricalDataEnveloppeServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(ValidatedHistoricalDataEnveloppeService.class);
        Optional<ValidatedHistoricalDataEnveloppeProvider> provider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataEnveloppeServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataEnveloppeServiceRegistrar(provider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(ValidatedHistoricalDataEnveloppeService.class);

        // When
        new ValidatedHistoricalDataEnveloppeServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(ValidatedHistoricalDataEnveloppeService.class);
        var mockProvider = mock(ValidatedHistoricalDataEnveloppeProvider.class);

        // When
        new ValidatedHistoricalDataEnveloppeServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}
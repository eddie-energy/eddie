package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.core.services.AccountingPointEnvelopeService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountingPointEnvelopeServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(AccountingPointEnvelopeService.class);
        Optional<AccountingPointEnvelopeProvider> provider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new AccountingPointEnvelopeServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class,
                     () -> new AccountingPointEnvelopeServiceRegistrar(provider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(AccountingPointEnvelopeService.class);

        // When
        new AccountingPointEnvelopeServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(AccountingPointEnvelopeService.class);
        var mockProvider = mock(AccountingPointEnvelopeProvider.class);

        // When
        new AccountingPointEnvelopeServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}

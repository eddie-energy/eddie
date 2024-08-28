package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.core.services.AccountingPointEnveloppeService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountingPointEnveloppeServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(AccountingPointEnveloppeService.class);
        Optional<AccountingPointEnveloppeProvider> provider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new AccountingPointEnveloppeServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class,
                     () -> new AccountingPointEnveloppeServiceRegistrar(provider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(AccountingPointEnveloppeService.class);

        // When
        new AccountingPointEnveloppeServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(AccountingPointEnveloppeService.class);
        var mockProvider = mock(AccountingPointEnveloppeProvider.class);

        // When
        new AccountingPointEnveloppeServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}

package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.core.services.AccountingPointEnvelopeService;
import energy.eddie.spring.regionconnector.extensions.AccountingPointEnvelopeServiceRegistrar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
class AccountingPointDataOutboundRegistrarTest {
    @Mock
    private AccountingPointEnvelopeOutboundConnector apConnector;
    @Mock
    private AccountingPointEnvelopeService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(apConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new AccountingPointDataOutboundRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new AccountingPointDataOutboundRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new AccountingPointEnvelopeServiceRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new AccountingPointDataOutboundRegistrar(Optional.of(apConnector), service);

        // Then
        verify(service).getAccountingPointEnvelopeStream();
        verify(apConnector).setAccountingPointEnvelopeStream(any());
    }
}
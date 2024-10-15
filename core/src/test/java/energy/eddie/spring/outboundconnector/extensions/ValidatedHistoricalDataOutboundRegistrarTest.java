package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.core.services.ValidatedHistoricalDataEnvelopeService;
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
class ValidatedHistoricalDataOutboundRegistrarTest {
    @Mock
    private ValidatedHistoricalDataEnvelopeOutboundConnector apConnector;
    @Mock
    private ValidatedHistoricalDataEnvelopeService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(apConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataOutboundRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new ValidatedHistoricalDataOutboundRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new ValidatedHistoricalDataOutboundRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new ValidatedHistoricalDataOutboundRegistrar(Optional.of(apConnector), service);

        // Then
        verify(service).getEddieValidatedHistoricalDataMarketDocumentStream();
        verify(apConnector).setEddieValidatedHistoricalDataMarketDocumentStream(any());
    }
}
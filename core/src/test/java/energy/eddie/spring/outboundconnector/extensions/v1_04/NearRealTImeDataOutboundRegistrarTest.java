package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_04.NearRealTimeDataMarketDocumentService;
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
class NearRealTImeDataOutboundRegistrarTest {
    @Mock
    private NearRealTimeDataMarketDocumentOutboundConnector rtdConnector;
    @Mock
    private NearRealTimeDataMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(rtdConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrar(Optional.of(rtdConnector), service);

        // Then
        verify(service).getNearRealTimeDataMarketDocumentStream();
        verify(rtdConnector).setNearRealTimeDataMarketDocumentStream(any());
    }
}
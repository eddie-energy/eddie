package energy.eddie.spring.outboundconnector.extensions.v1_06;

import energy.eddie.api.v1_06.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_06;
import energy.eddie.core.services.v1_06.NearRealTimeDataMarketDocumentService;
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
class NearRealTImeDataOutboundRegistrarV1_06Test {
    @Mock
    private NearRealTimeDataMarketDocumentOutboundConnectorV1_06 rtdConnector;
    @Mock
    private NearRealTimeDataMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(rtdConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrarV1_06(null, service));
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrarV1_06(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrarV1_06(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrarV1_06(Optional.of(rtdConnector), service);

        // Then
        verify(service).getNearRealTimeDataMarketDocumentStream();
        verify(rtdConnector).setNearRealTimeDataMarketDocumentStreamV1_06(any());
    }
}
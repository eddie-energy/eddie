package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.v1_12.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_12;
import energy.eddie.core.services.v1_12.NearRealTimeDataMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings({"DataFlowIssue", "java:S114"})
@ExtendWith(MockitoExtension.class)
class NearRealTImeDataOutboundRegistrarV1_12Test {
    @Mock
    private NearRealTimeDataMarketDocumentOutboundConnectorV1_12 rtdConnector;
    @Mock
    private NearRealTimeDataMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(rtdConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrarV1_12(null, service));
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrarV1_12(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrarV1_12(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrarV1_12(Optional.of(rtdConnector), service);

        // Then
        verify(service).getNearRealTimeDataMarketDocumentStream();
        verify(rtdConnector).setNearRealTimeDataMarketDocumentStreamV1_12(any());
    }
}
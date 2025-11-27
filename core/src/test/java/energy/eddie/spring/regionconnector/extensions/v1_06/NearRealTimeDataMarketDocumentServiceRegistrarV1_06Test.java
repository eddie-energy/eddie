package energy.eddie.spring.regionconnector.extensions.v1_06;

import energy.eddie.api.v1_06.NearRealTimeDataMarketDocumentProviderV1_06;
import energy.eddie.core.services.v1_06.NearRealTimeDataMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NearRealTimeDataMarketDocumentServiceRegistrarV1_06Test {
    @Mock
    private NearRealTimeDataMarketDocumentService service;
    @Mock
    private ObjectProvider<NearRealTimeDataMarketDocumentProviderV1_06> objectProvider;

    @Test
    void givenProvider_registers() {
        // Given
        doNothing().when(objectProvider).ifAvailable(any());

        // When
        new NearRealTimeDataMarketDocumentServiceRegistrarV1_06(objectProvider, service);

        // Then
        verify(objectProvider).ifAvailable(any());
    }
}
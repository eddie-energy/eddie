package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.v1_12.NearRealTimeDataMarketDocumentProviderV1_12;
import energy.eddie.core.services.v1_12.NearRealTimeDataMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("java:S114")
class NearRealTimeDataMarketDocumentServiceRegistrarV1_12Test {
    @Mock
    private NearRealTimeDataMarketDocumentService service;
    @Mock
    private ObjectProvider<NearRealTimeDataMarketDocumentProviderV1_12> objectProvider;

    @Test
    void givenProvider_registers() {
        // Given
        doNothing().when(objectProvider).ifAvailable(any());

        // When
        new NearRealTimeDataMarketDocumentServiceRegistrarV1_12(objectProvider, service);

        // Then
        verify(objectProvider).ifAvailable(any());
    }
}
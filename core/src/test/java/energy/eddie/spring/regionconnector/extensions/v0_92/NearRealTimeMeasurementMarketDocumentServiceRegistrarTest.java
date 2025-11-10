package energy.eddie.spring.regionconnector.extensions.v0_92;

import energy.eddie.api.v0_92.NearRealTimeMeasurementMarketDocumentProvider;
import energy.eddie.core.services.v0_92.NearRealTimeMeasurementMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NearRealTimeMeasurementMarketDocumentServiceRegistrarTest {
    @Mock
    private ObjectProvider<NearRealTimeMeasurementMarketDocumentProvider> objectProvider;

    @Test
    void givenProvider_registers() {
        // Given
        var service = new NearRealTimeMeasurementMarketDocumentService();
        doNothing().when(objectProvider).ifAvailable(any());

        // When
        new NearRealTimeMeasurementMarketDocumentServiceRegistrar(objectProvider, service);

        // Then
        verify(objectProvider).ifAvailable(any());
    }
}
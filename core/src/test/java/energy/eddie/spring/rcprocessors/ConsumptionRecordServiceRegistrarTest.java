package energy.eddie.spring.rcprocessors;

import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.core.services.ConsumptionRecordService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ConsumptionRecordServiceRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var mockService = mock(ConsumptionRecordService.class);
        Optional<Mvp1ConsumptionRecordProvider> mockProvider = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new ConsumptionRecordServiceRegistrar(null, mockService));
        assertThrows(NullPointerException.class, () -> new ConsumptionRecordServiceRegistrar(mockProvider, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        var mockService = mock(ConsumptionRecordService.class);

        // When
        new ConsumptionRecordServiceRegistrar(Optional.empty(), mockService);

        // Then
        verifyNoInteractions(mockService);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        var mockService = mock(ConsumptionRecordService.class);
        var mockProvider = mock(Mvp1ConsumptionRecordProvider.class);

        // When
        new ConsumptionRecordServiceRegistrar(Optional.of(mockProvider), mockService);

        // Then
        verify(mockService).registerProvider(mockProvider);
    }
}

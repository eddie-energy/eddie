package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GreenButtonRegionConnectorTest {
    @Mock
    private GreenButtonApi apiClient;
    @InjectMocks
    private GreenButtonRegionConnector regionConnector;


    @Test
    void testGetMetadata_returnsMetadata() {
        // Given
        // When
        var res = regionConnector.getMetadata();

        // Then
        assertEquals(GreenButtonRegionConnectorMetadata.getInstance(), res);
    }

    @Test
    void testHealth_returnsHealth() {
        // Given
        when(apiClient.health())
                .thenReturn(Mono.just(Map.of("ServiceStatusApi", HealthState.UP)));

        // When
        var res = regionConnector.health();

        // Then
        assertEquals(Map.of("ServiceStatusApi", HealthState.UP), res);
    }
}
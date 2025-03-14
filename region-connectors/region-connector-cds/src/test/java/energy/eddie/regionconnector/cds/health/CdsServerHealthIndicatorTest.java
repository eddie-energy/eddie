package energy.eddie.regionconnector.cds.health;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsServerHealthIndicatorTest {
    @Spy
    @SuppressWarnings("unused")
    private final CdsServer cdsServer = new CdsServerBuilder().setBaseUri("https://localhost").build();
    @Mock
    private CdsPublicApis apis;
    @InjectMocks
    private CdsServerHealthIndicator healthIndicator;

    @Test
    void testHealth_onConnectionToCdsServer_returnsUp() {
        // Given
        when(apis.carbonDataSpec(any()))
                .thenReturn(Mono.just(new CarbonDataSpec200Response()));

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(Health.up().build(), res);
    }

    @Test
    void testHealth_onError_returnsDown() {
        // Given
        var error = new RuntimeException();
        when(apis.carbonDataSpec(any()))
                .thenReturn(Mono.error(error));

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(Health.down(error).build(), res);
    }

    @Test
    void testHealth_onServiceUnavailable_returnsOutOfService() {
        // Given
        var error = WebClientResponseException.create(HttpStatus.SERVICE_UNAVAILABLE, "status", null, null, null, null);
        when(apis.carbonDataSpec(any()))
                .thenReturn(Mono.error(error));

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(Health.outOfService().build(), res);
    }
}
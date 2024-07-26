package energy.eddie.regionconnector.dk.energinet.health;

import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnerginetApiHealthIndicatorTest {
    @Mock
    private EnerginetCustomerApi api;
    @InjectMocks
    private EnerginetApiHealthIndicator indicator;

    @Test
    void healthIsUp_whenApiIsReachable() {
        // Given
        when(api.isAlive()).thenReturn(Mono.just(Boolean.TRUE));

        // When
        var res = indicator.health();

        // Then
        assertEquals(Status.UP, res.getStatus());
    }

    @Test
    void healthIsDown_whenApiReturnsFalse() {
        // Given
        when(api.isAlive()).thenReturn(Mono.just(Boolean.FALSE));

        // When
        var res = indicator.health();

        // Then
        assertEquals(Status.DOWN, res.getStatus());
    }

    @Test
    void healthIsDown_whenApiThrows() {
        // Given
        when(api.isAlive())
                .thenReturn(
                        Mono.error(
                                WebClientResponseException.create(
                                        HttpStatus.INTERNAL_SERVER_ERROR, "text", null, null, null, null
                                )
                        )
                );

        // When
        var res = indicator.health();

        // Then
        assertEquals(Status.DOWN, res.getStatus());
    }
}
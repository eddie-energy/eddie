package energy.eddie.regionconnector.be.fluvius.health;

import energy.eddie.regionconnector.be.fluvius.client.FluviusApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluviusApiHealthIndicatorTest {
    @Mock
    private FluviusApiClient api;
    @InjectMocks
    private FluviusApiHealthIndicator healthIndicator;

    @Test
    void testHealth() {
        // Given
        when(api.health()).thenReturn(Health.up().build());

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(Health.up().build(), res);
    }
}
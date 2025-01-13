package energy.eddie.regionconnector.simulation.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SimulationHealthIndicatorTest {
    @InjectMocks
    private SimulationHealthIndicator healthIndicator;

    @Test
    void testHealth() {
        // Given
        // When
        var health = healthIndicator.health();

        // Then
        assertEquals(Health.up().build(), health);
    }
}
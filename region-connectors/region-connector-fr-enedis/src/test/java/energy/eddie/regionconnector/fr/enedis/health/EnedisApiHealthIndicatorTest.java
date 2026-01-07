package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.regionconnector.fr.enedis.api.EnedisHealth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnedisApiHealthIndicatorTest {

    @Mock
    private EnedisHealth api;

    @Test
    void healthReturnsCorrectApiHealth() {
        // Given
        var healthIndicator = new EnedisApiHealthIndicator(api, "contractApi");
        when(api.health())
                .thenReturn(Map.of("contractApi", Health.up().build(), "authApi", Health.down().build()));

        // When
        var res = healthIndicator.health();

        // Then
        assertNotNull(res);
        assertEquals(Status.UP, res.getStatus());
    }
}

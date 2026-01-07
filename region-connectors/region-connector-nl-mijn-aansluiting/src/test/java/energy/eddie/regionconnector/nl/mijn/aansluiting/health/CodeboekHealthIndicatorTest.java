package energy.eddie.regionconnector.nl.mijn.aansluiting.health;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.CodeboekApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeboekHealthIndicatorTest {
    @Mock
    private CodeboekApiClient client;
    @InjectMocks
    private CodeboekHealthIndicator healthIndicator;

    @Test
    void health_returnsHealth() {
        // Given
        when(client.health()).thenReturn(Health.up().build());

        // When
        var res = healthIndicator.health();

        // Then
        assertNotNull(res);
        assertEquals(Status.UP, res.getStatus());
    }
}
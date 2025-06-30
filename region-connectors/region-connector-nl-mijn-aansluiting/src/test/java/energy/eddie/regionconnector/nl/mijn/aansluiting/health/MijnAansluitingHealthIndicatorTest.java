package energy.eddie.regionconnector.nl.mijn.aansluiting.health;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MijnAansluitingHealthIndicatorTest {

    @Test
    void healthReturnsUnknownInitially() {
        // Given
        var api = new ApiClient(WebClient.builder());
        var healthIndicator = new MijnAansluitingHealthIndicator(api);

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(Status.UNKNOWN, res.getStatus());
    }
}
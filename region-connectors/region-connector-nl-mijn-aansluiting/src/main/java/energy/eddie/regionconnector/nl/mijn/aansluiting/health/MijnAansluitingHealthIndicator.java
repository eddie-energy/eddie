package energy.eddie.regionconnector.nl.mijn.aansluiting.health;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.ApiClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MijnAansluitingHealthIndicator implements HealthIndicator {
    private final ApiClient api;

    public MijnAansluitingHealthIndicator(ApiClient api) {
        this.api = api;
    }

    @Override
    public Health health() {
        return api.health();
    }
}

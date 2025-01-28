package energy.eddie.regionconnector.be.fluvius.health;

import energy.eddie.regionconnector.be.fluvius.clients.FluviusApiClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FluviusApiHealthIndicator implements HealthIndicator {
    private final FluviusApiClient api;

    public FluviusApiHealthIndicator(FluviusApiClient api) {
        this.api = api;
    }

    @Override
    public Health health() {
        return api.health();
    }
}

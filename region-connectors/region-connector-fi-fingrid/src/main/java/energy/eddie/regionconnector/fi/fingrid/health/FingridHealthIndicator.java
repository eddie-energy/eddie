package energy.eddie.regionconnector.fi.fingrid.health;

import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FingridHealthIndicator implements HealthIndicator {
    private final FingridApiClient api;

    public FingridHealthIndicator(FingridApiClient api) {this.api = api;}

    @Override
    public Health health() {
        return api.health();
    }
}

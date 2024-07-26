package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class EnedisApiHealthIndicator implements HealthIndicator {
    private final EnedisApi api;
    private final String apiName;

    EnedisApiHealthIndicator(EnedisApi api, String apiName) {
        this.api = api;
        this.apiName = apiName;
    }

    @Override
    public Health health() {
        return (api.health().get(apiName) == HealthState.UP
                ? Health.up()
                : Health.down())
                .build();
    }
}

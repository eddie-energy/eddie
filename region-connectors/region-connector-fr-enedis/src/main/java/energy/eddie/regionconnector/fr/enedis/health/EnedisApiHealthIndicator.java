package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class EnedisApiHealthIndicator implements HealthIndicator {
    private final EnedisHealth enedisHealth;
    private final String apiName;

    EnedisApiHealthIndicator(EnedisHealth enedisHealth, String apiName) {
        this.enedisHealth = enedisHealth;
        this.apiName = apiName;
    }

    @Override
    public Health health() {
        return (enedisHealth.health().get(apiName) == HealthState.UP
                ? Health.up()
                : Health.down())
                .build();
    }
}

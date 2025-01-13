package energy.eddie.regionconnector.simulation.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SimulationHealthIndicator implements HealthIndicator {

    /**
     * With no additional dependencies we can always assume the simulation region connector to be healthy when enabled.
     *
     * @return up if enabled
     */
    @Override
    public Health health() {
        return Health.up().build();
    }
}

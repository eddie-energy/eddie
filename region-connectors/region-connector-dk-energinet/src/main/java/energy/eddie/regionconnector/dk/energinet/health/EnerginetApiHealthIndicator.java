package energy.eddie.regionconnector.dk.energinet.health;

import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EnerginetApiHealthIndicator implements HealthIndicator {
    private final EnerginetCustomerApi api;

    public EnerginetApiHealthIndicator(EnerginetCustomerApi api) {
        this.api = api;
    }

    @Override
    public Health health() {
        return api.isAlive()
                  .map(isAlive -> Boolean.TRUE.equals(isAlive) ? Health.up() : Health.down())
                  .onErrorResume(Exception.class, e -> Mono.just(Health.down(e)))
                  .onErrorResume(e -> Mono.just(Health.down()))
                  .map(Health.Builder::build)
                  .block();
    }
}

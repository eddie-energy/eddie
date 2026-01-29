package energy.eddie.regionconnector.dk.energinet.health;

import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EnerginetApiHealthIndicator implements HealthIndicator {
    private final EnerginetCustomerApiClient api;

    public EnerginetApiHealthIndicator(EnerginetCustomerApiClient api) {
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

package energy.eddie.regionconnector.us.green.button.health;

import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GreenButtonApiHealthIndicator implements HealthIndicator {
    private final GreenButtonApi greenButtonApi;

    public GreenButtonApiHealthIndicator(
            GreenButtonApi greenButtonApi
    ) {
        this.greenButtonApi = greenButtonApi;
    }

    @Override
    public Health health() {
        return greenButtonApi.isAlive()
                             .map(isAlive -> Boolean.TRUE.equals(isAlive) ? Health.up() : Health.down())
                             .onErrorResume(Exception.class, e -> Mono.just(Health.down(e)))
                             .onErrorResume(e -> Mono.just(Health.down()))
                             .map(Health.Builder::build)
                             .block();
    }
}

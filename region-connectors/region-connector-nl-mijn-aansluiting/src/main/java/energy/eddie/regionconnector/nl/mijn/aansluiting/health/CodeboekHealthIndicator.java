package energy.eddie.regionconnector.nl.mijn.aansluiting.health;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.CodeboekApiClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CodeboekHealthIndicator implements HealthIndicator {
    private final CodeboekApiClient codeboekApiClient;

    public CodeboekHealthIndicator(CodeboekApiClient codeboekApiClient) {this.codeboekApiClient = codeboekApiClient;}

    @Override
    public Health health() {
        return codeboekApiClient.health();
    }
}

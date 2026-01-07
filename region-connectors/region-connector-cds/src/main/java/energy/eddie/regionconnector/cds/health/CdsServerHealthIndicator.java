package energy.eddie.regionconnector.cds.health;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class CdsServerHealthIndicator implements HealthIndicator {
    private final CdsPublicApis apis;
    private final CdsServer cdsServer;

    public CdsServerHealthIndicator(CdsPublicApis apis, CdsServer cdsServer) {
        this.apis = apis;
        this.cdsServer = cdsServer;
    }

    @Override
    public Health health() {
        try {
            apis.carbonDataSpec(cdsServer.baseUri()).block();
            return Health.up().build();
        } catch (WebClientResponseException.ServiceUnavailable  e) {
            return Health.outOfService().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}

package energy.eddie.spring.rcprocessors;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.HealthService;
import energy.eddie.spring.RegionConnectorProcessor;

import static java.util.Objects.requireNonNull;

@RegionConnectorProcessor
public class HealthServiceRegistrar {
    public HealthServiceRegistrar(RegionConnector regionConnector, HealthService healthService) {
        requireNonNull(regionConnector);
        requireNonNull(healthService);
        healthService.registerRegionConnector(regionConnector);
    }
}

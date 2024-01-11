package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.HealthService;

import static java.util.Objects.requireNonNull;

/**
 * The {@code HealthServiceRegistrar} will be added to each region connector's own context and will
 * register the {@link RegionConnector} of each region connector to the common {@link HealthService}.
 * Each region connector implementation is required to provide an implementation of the {@code RegionConnector} interface.
 */
@RegionConnectorExtension
public class HealthServiceRegistrar {
    public HealthServiceRegistrar(RegionConnector regionConnector, HealthService healthService) {
        requireNonNull(regionConnector);
        requireNonNull(healthService);
        healthService.registerRegionConnector(regionConnector);
    }
}

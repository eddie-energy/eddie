package energy.eddie.spring.regionconnector.extensions;


import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.MetadataService;
import energy.eddie.spring.RegionConnectorExtension;

import static java.util.Objects.requireNonNull;

/**
 * The {@code MetadataServiceRegistrar} should be added to each region connector's own context and will
 * register the {@link RegionConnector} of each region connector to the common {@link MetadataService}.
 * Each region connector implementation is required to provide an implementation of the {@code RegionConnector} interface.
 */
@RegionConnectorExtension
public class MetadataServiceRegistrar {
    public MetadataServiceRegistrar(RegionConnector regionConnector, MetadataService metadataService) {
        requireNonNull(regionConnector);
        requireNonNull(metadataService);
        metadataService.registerRegionConnector(regionConnector);
    }
}

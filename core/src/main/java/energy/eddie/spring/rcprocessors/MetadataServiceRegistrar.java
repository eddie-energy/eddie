package energy.eddie.spring.rcprocessors;


import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.MetadataService;
import energy.eddie.spring.RegionConnectorProcessor;

import static java.util.Objects.requireNonNull;

@RegionConnectorProcessor
public class MetadataServiceRegistrar {
    public MetadataServiceRegistrar(RegionConnector regionConnector, MetadataService metadataService) {
        requireNonNull(regionConnector);
        requireNonNull(metadataService);
        metadataService.registerRegionConnector(regionConnector);
    }
}

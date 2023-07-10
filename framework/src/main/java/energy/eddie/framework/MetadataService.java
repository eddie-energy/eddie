package energy.eddie.framework;

import com.google.inject.Inject;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;

import java.util.Collection;
import java.util.Set;

public class MetadataService {

    private final Set<RegionConnector> regionConnectors;

    @Inject
    public MetadataService(Set<RegionConnector> regionConnectors) {
        this.regionConnectors = regionConnectors;
    }

    public Collection<RegionConnectorMetadata> getRegionConnectorMetadata() {
        return regionConnectors.stream()
                .map(RegionConnector::getMetadata)
                .toList();
    }
}

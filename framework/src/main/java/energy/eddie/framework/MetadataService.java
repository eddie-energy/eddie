package energy.eddie.framework;

import com.google.inject.Inject;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0.RegionConnector;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataService {

    @Inject
    private Set<RegionConnector> regionConnectors;

    public Collection<RegionConnectorMetadata> getRegionConnectorMetadata() {
        return regionConnectors.stream()
                .map(RegionConnector::getMetadata)
                .collect(Collectors.toList());
    }
}

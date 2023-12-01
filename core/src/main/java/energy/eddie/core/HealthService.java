package energy.eddie.core;

import com.google.inject.Inject;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HealthService {
    private final Set<RegionConnector> regionConnectors;

    @Inject
    public HealthService(Set<RegionConnector> regionConnectors) {
        this.regionConnectors = regionConnectors;
    }

    public Map<String, Map<String, HealthState>> getRegionConnectorHealth() {
        return regionConnectors.stream()
                .collect(Collectors.toMap(rc -> rc.getMetadata().mdaDisplayName(), RegionConnector::health));
    }
}

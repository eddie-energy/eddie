package energy.eddie.core.services;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HealthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthService.class);
    private final Set<RegionConnector> regionConnectors = new HashSet<>();

    public void registerRegionConnector(RegionConnector regionConnector) {
        LOGGER.info("HealthService: Registering {}", regionConnector.getClass().getName());
        regionConnectors.add(regionConnector);
    }

    public Map<String, Map<String, HealthState>> getRegionConnectorHealth() {
        return regionConnectors.stream()
                .collect(Collectors.toMap(rc -> rc.getMetadata().id(), RegionConnector::health));
    }
}

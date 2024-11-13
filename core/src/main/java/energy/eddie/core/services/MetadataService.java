package energy.eddie.core.services;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.dtos.SupportedDataNeeds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MetadataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataService.class);
    private final Set<RegionConnector> regionConnectors = new HashSet<>();

    public void registerRegionConnector(RegionConnector regionConnector) {
        LOGGER.info("MetadataService: Registering {}", regionConnector.getClass().getName());
        regionConnectors.add(regionConnector);
    }

    public List<RegionConnectorMetadata> getRegionConnectorMetadata() {
        return regionConnectors.stream()
                               .map(RegionConnector::getMetadata)
                               .toList();
    }


    public List<SupportedDataNeeds> getSupportedDataNeeds() {
        List<SupportedDataNeeds> supportedDataNeeds = new ArrayList<>();

        for (var metadata : getRegionConnectorMetadata()) {
            List<String> dataNeeds = metadata.supportedDataNeeds().stream()
                                             .map(Class::getSimpleName)
                                             .toList();
            supportedDataNeeds.add(new SupportedDataNeeds(metadata.id(), dataNeeds));
        }
        return supportedDataNeeds;
    }
}

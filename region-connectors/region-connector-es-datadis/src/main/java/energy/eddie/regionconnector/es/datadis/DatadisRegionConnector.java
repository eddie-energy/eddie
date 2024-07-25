package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class DatadisRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisRegionConnector.class);
    private final PermissionRequestService permissionRequestService;

    public DatadisRegionConnector(PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return DatadisRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        try {
            permissionRequestService.terminatePermission(permissionId);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Got request to terminate permission with ID {}, but it couldn't be found", permissionId);
        }
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of();
    }
}
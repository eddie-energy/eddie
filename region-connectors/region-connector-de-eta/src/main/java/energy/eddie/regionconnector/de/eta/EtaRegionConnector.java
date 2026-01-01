package energy.eddie.regionconnector.de.eta;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata.REGION_CONNECTOR_ID;

/**
 * Main Region Connector implementation for Germany (ETA Plus).
 * This class serves as the entry point for the region connector and handles
 * permission termination requests.
 */
@Component
public class EtaRegionConnector implements energy.eddie.api.v0.RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaRegionConnector.class);
    
    private final DePermissionRequestRepository repository;
    private final Outbox outbox;

    @Autowired
    public EtaRegionConnector(
            DePermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.repository = repository;
        this.outbox = outbox;
        LOGGER.info("Initialized {} region connector", REGION_CONNECTOR_ID);
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EtaRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        
        // Mark the permission as terminated in the database
        // The actual termination will be handled by a scheduled task or event processor
        // that reads from the outbox and sends termination requests to ETA Plus

        // Implement actual termination logic with ETA Plus API in GH-2197 by developer bilal-sakhawat1
        // For now, we just mark it as terminated and requiring external termination
        
        // Publish termination events
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
        
        LOGGER.info("Permission {} marked for termination", permissionId);
    }
}

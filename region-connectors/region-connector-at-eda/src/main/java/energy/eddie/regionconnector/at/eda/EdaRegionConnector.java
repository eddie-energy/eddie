package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class EdaRegionConnector implements RegionConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnector.class);
    private final EdaAdapter edaAdapter;
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    @Autowired
    public EdaRegionConnector(
            EdaAdapter edaAdapter,
            AtPermissionRequestRepository repository,
            Outbox outbox
    ) throws TransmissionException {
        this.edaAdapter = edaAdapter;
        this.repository = repository;
        this.outbox = outbox;
        edaAdapter.start();
    }

    @Override
    public void close() throws Exception {
        edaAdapter.close();
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EdaRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION));
    }
}

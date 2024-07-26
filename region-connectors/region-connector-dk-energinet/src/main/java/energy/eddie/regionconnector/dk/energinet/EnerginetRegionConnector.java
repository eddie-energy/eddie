package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static java.util.Objects.requireNonNull;

@Component
public class EnerginetRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetRegionConnector.class);
    private final DkPermissionRequestRepository repository;
    private final Outbox outbox;

    public EnerginetRegionConnector(
            DkPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.repository = requireNonNull(repository);
        this.outbox = outbox;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EnerginetRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var permissionRequest = repository.findByPermissionId(permissionId);
        if (permissionRequest.isEmpty() || permissionRequest.get().status() != PermissionProcessStatus.ACCEPTED) {
            return;
        }
        outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
    }
}

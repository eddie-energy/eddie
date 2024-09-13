package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class EdaRegionConnector implements RegionConnector, ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnector.class);
    private final EdaAdapter edaAdapter;
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;

    @Autowired
    public EdaRegionConnector(
            EdaAdapter edaAdapter,
            AtPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> permissionStateMessages,
            Outbox outbox
    ) throws TransmissionException {
        this.edaAdapter = edaAdapter;
        this.repository = repository;
        this.permissionStateMessages = permissionStateMessages;
        this.outbox = outbox;
        edaAdapter.start();
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return permissionStateMessages.asFlux();
    }

    @Override
    public void close() throws Exception {
        edaAdapter.close();
        permissionStateMessages.tryEmitComplete();
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

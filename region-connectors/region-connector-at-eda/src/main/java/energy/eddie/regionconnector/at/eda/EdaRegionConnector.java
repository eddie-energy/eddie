package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import energy.eddie.api.v0.*;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.TerminationEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class EdaRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnector.class);
    private final EdaAdapter edaAdapter;
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;
    private final AtConfiguration atConfiguration;

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;

    @Autowired
    public EdaRegionConnector(
            EdaAdapter edaAdapter,
            AtPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> permissionStateMessages,
            Outbox outbox, AtConfiguration atConfiguration
    ) throws TransmissionException {
        this.edaAdapter = edaAdapter;
        this.repository = repository;
        this.permissionStateMessages = permissionStateMessages;
        this.outbox = outbox;
        this.atConfiguration = atConfiguration;
        edaAdapter.start();
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return permissionStateMessages.asFlux();
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EdaRegionConnectorMetadata.getInstance();
    }

    @Override
    public Map<String, HealthState> health() {
        return edaAdapter.health();
    }

    @Override
    public void close() throws Exception {
        edaAdapter.close();
        permissionStateMessages.tryEmitComplete();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var request = repository.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        AtPermissionRequest permissionRequest = request.get();
        CMRevoke revoke = new CCMORevoke(permissionRequest, atConfiguration.eligiblePartyId()).toCMRevoke();
        try {
            edaAdapter.sendCMRevoke(revoke);
        } catch (Exception e) {
            LOGGER.warn("Error trying to terminate permission request.", e);
        }
        outbox.commit(new TerminationEvent(permissionId, revoke.getProcessDirectory().getReason()));
    }
}

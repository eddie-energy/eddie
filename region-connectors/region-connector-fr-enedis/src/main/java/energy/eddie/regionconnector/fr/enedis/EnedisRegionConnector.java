package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.*;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class EnedisRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRegionConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final EnedisApi enedisApi;
    private final PermissionRequestService permissionRequestService;

    public EnedisRegionConnector(
            EnedisApi enedisApi,
            PermissionRequestService permissionRequestService,
            Sinks.Many<ConnectionStatusMessage> connectionStatusSink
    ) {
        this.enedisApi = enedisApi;
        this.permissionRequestService = permissionRequestService;
        this.connectionStatusSink = connectionStatusSink;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EnedisRegionConnectorMetadata.getInstance();
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusSink.asFlux();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var permissionRequest = permissionRequestService.findPermissionRequestByPermissionId(permissionId);
        if (permissionRequest.isEmpty()) {
            return;
        }
        try {
            permissionRequest.get().terminate();
        } catch (StateTransitionException e) {
            LOGGER.error("PermissionRequest with permissionID {} cannot be revoked", permissionId, e);
        }
    }

    @Override
    public Map<String, HealthState> health() {
        return enedisApi.health();
    }

    @Override
    public void close() {
        connectionStatusSink.tryEmitComplete();
    }
}

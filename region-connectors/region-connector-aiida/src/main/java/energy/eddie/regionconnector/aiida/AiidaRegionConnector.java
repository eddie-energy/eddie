package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Flow;

public class AiidaRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnector.class);
    private final int port;
    private final AiidaRegionConnectorService aiidaService;

    public AiidaRegionConnector(int port, AiidaRegionConnectorService aiidaService) {
        this.port = port;
        this.aiidaService = aiidaService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return AiidaRegionConnectorMetadata.getInstance();
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(aiidaService.connectionStatusMessageFlux());
    }

    @Override
    public void terminatePermission(String permissionId) {
        try {
            aiidaService.terminatePermission(permissionId);
        } catch (StateTransitionException e) {
            LOGGER.error("Error while terminating permission {}", permissionId, e);
        }
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        LOGGER.info("Called startWebapp for AIIDA with address {}, internal port is {}", address, port);
        return port;
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of(getMetadata().id(), HealthState.UP);
    }

    @Override
    public void close() {
        aiidaService.close();
    }
}

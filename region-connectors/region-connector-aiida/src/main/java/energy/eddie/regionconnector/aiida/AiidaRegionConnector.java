package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.*;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Flow;

public class AiidaRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    public static final String COUNTRY_CODE = "aiida";
    public static final String MDA_CODE = "aiida";
    public static final String MDA_DISPLAY_NAME = "AIIDA";
    public static final String BASE_PATH = "/region-connectors/aiida/";
    public static final int COVERED_METERING_POINTS = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnector.class);
    private final int port;
    private final AiidaRegionConnectorService aiidaService;

    public AiidaRegionConnector(int port, AiidaRegionConnectorService aiidaService) {
        this.port = port;
        this.aiidaService = aiidaService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(aiidaService.connectionStatusMessageFlux());
    }

    @Override
    public void terminatePermission(String permissionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        LOGGER.info("Called startWebapp for AIIDA with address {}, internal port is {}", address, port);
        return port;
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of(MDA_CODE, HealthState.UP);
    }

    @Override
    public void close() {
        aiidaService.close();
    }
}

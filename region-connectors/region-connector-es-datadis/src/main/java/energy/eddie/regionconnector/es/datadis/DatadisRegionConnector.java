package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Flow;

public class DatadisRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider,
        Mvp1ConsumptionRecordProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisRegionConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink;
    private final PermissionRequestService permissionRequestService;
    private final int port;

    public DatadisRegionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink,
            PermissionRequestService permissionRequestService,
            int port) {
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.consumptionRecordSink = consumptionRecordSink;
        this.permissionRequestService = permissionRequestService;
        this.port = port;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return DatadisRegionConnectorMetadata.getInstance();
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(connectionStatusMessageSink.asFlux());
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordSink.asFlux());
    }

    @Override
    public void terminatePermission(String permissionId) {
        try {
            permissionRequestService.terminatePermission(permissionId);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Got request to terminate permission with ID {}, but it couldn't be found", permissionId);
        } catch (StateTransitionException e) {
            LOGGER.error("Error while terminating a permission request", e);
        }
    }

    @Override
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        LOGGER.info("Called startWebapp for Datadis with address {}, internal port is {}", address, port);
        return port;
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of("permissionRequestRepository", HealthState.UP);
    }

    @Override
    public void close() {
        connectionStatusMessageSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
    }
}
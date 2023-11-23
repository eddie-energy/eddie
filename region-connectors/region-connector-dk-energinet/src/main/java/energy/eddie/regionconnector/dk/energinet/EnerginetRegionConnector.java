package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

public class EnerginetRegionConnector implements RegionConnector {
    public static final String BASE_PATH = "/region-connectors/dk-energinet/";
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 24;
    private static final String COUNTRY_CODE = "dk";
    private static final String MDA_CODE = COUNTRY_CODE + "-energinet";
    private static final String MDA_DISPLAY_NAME = "Denmark ENERGINET";
    private static final int COVERED_METERING_POINTS = 36951446; // TODO: Evaluate covered metering points
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetRegionConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink;
    private final EnerginetCustomerApi energinetCustomerApi;
    private final ConcurrentMap<String, ConnectionStatusMessage> permissionIdToConnectionStatusMessages = new ConcurrentHashMap<>();
    private final DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository;
    private final int port;

    public EnerginetRegionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink,
            EnerginetCustomerApi energinetCustomerApi,
            DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository, int port) {
        this.connectionStatusSink = connectionStatusSink;
        this.consumptionRecordSink = consumptionRecordSink;
        this.energinetCustomerApi = requireNonNull(energinetCustomerApi);
        this.permissionRequestRepository = requireNonNull(permissionRequestRepository);
        this.port = port;

        this.connectionStatusSink.asFlux().subscribe(connectionStatusMessage -> {
            var permissionId = connectionStatusMessage.permissionId();
            LOGGER.info("Received connectionStatusMessage for permissionId '{}': {}", permissionId, connectionStatusMessage);
            if (permissionId != null) {
                permissionIdToConnectionStatusMessages.put(permissionId, connectionStatusMessage);
            }
        });
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return new RegionConnectorMetadata(MDA_CODE, MDA_DISPLAY_NAME, COUNTRY_CODE, BASE_PATH, COVERED_METERING_POINTS);
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(connectionStatusSink.asFlux());
    }

    @Override
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordSink.asFlux());
    }

    @Override
    public void terminatePermission(String permissionId) {
        var permissionRequest = permissionRequestRepository.findByPermissionId(permissionId);
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
    public int startWebapp(InetSocketAddress address, boolean devMode) {
        LOGGER.info("Called startWebapp for EnerginetRegionConnector with address {}, internal port is: {}", address, port);
        return port;
    }

    @Override
    public Map<String, HealthState> health() {
        return energinetCustomerApi.health();
    }

    @Override
    public void close() {
        connectionStatusSink.tryEmitComplete();
        consumptionRecordSink.tryEmitComplete();
    }
}

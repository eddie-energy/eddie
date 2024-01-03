package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.Flow;

@Component
public class EnedisRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider,
        Mvp1ConsumptionRecordProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRegionConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final Sinks.Many<ConsumptionRecord> consumptionRecordSink;
    private final EnedisApi enedisApi;
    private final PermissionRequestService permissionRequestService;

    public EnedisRegionConnector(
            EnedisApi enedisApi,
            PermissionRequestService permissionRequestService,
            Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink
    ) {
        this.enedisApi = enedisApi;
        this.permissionRequestService = permissionRequestService;
        this.connectionStatusSink = connectionStatusSink;
        this.consumptionRecordSink = consumptionRecordSink;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EnedisRegionConnectorMetadata.getInstance();
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
        consumptionRecordSink.tryEmitComplete();
    }
}
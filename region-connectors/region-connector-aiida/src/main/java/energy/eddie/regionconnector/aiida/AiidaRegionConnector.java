package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.*;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class AiidaRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider, ConsentMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink;

    public AiidaRegionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink
    ) {
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.consentMarketDocumentSink = consentMarketDocumentSink;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return AiidaRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);

        // TODO send MQTT termination request --> GH-963
    }

    @Override
    public Map<String, HealthState> health() {
        // TODO could check if MQTT broker is reachable --> GH-964
        return Map.of(getMetadata().id(), HealthState.UP);
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return connectionStatusMessageSink.asFlux();
    }

    @Override
    public Flux<ConsentMarketDocument> getConsentMarketDocumentStream() {
        return consentMarketDocumentSink.asFlux();
    }

    @Override
    public void close() {
        connectionStatusMessageSink.tryEmitComplete();
        consentMarketDocumentSink.tryEmitComplete();
    }
}

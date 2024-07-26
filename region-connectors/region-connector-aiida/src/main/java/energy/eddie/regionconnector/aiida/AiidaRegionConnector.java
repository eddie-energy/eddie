package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AiidaRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider, ConsentMarketDocumentProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink;
    private final AiidaPermissionService aiidaPermissionService;

    public AiidaRegionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink,
            AiidaPermissionService aiidaPermissionService
    ) {
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.consentMarketDocumentSink = consentMarketDocumentSink;
        this.aiidaPermissionService = aiidaPermissionService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return AiidaRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        aiidaPermissionService.terminatePermission(permissionId);
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

package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AiidaRegionConnector implements RegionConnector, ConnectionStatusMessageProvider, PermissionMarketDocumentProvider {
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink;
    private final Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink;
    private final AiidaPermissionService aiidaPermissionService;

    public AiidaRegionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink,
            AiidaPermissionService aiidaPermissionService
    ) {
        this.connectionStatusMessageSink = connectionStatusMessageSink;
        this.permissionMarketDocumentSink = permissionMarketDocumentSink;
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
    public void close() {
        connectionStatusMessageSink.tryEmitComplete();
        permissionMarketDocumentSink.tryEmitComplete();
    }

    @Override
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return permissionMarketDocumentSink.asFlux();
    }
}

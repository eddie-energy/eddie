package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.*;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class EnedisRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRegionConnector.class);
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final FrPermissionRequestRepository repository;
    private final Outbox outbox;

    public EnedisRegionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
            FrPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.connectionStatusSink = connectionStatusSink;
        this.repository = repository;
        this.outbox = outbox;
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
        var permissionRequest = repository.findByPermissionId(permissionId);
        if (permissionRequest.isEmpty()) {
            return;
        }
        outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
    }

    @Override
    public void close() {
        connectionStatusSink.tryEmitComplete();
    }
}

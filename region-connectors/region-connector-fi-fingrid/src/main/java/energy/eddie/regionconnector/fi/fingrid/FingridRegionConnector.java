package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class FingridRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    @Override
    public RegionConnectorMetadata getMetadata() {
        return FingridRegionConnectorMetadata.INSTANCE;
    }

    @Override
    public void terminatePermission(String permissionId) {
        // TODO: GH-1151
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return Flux.empty();
    }

    @Override
    public void close() {
        // Not required
    }
}

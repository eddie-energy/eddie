package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.v0.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

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
    public Map<String, HealthState> health() {
        return Map.of();
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

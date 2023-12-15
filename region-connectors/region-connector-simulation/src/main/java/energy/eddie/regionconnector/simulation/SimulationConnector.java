package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SimulationConnector implements RegionConnector {
    @Override
    public RegionConnectorMetadata getMetadata() {
        return SimulationConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Map<String, HealthState> health() {
        return Map.of(getMetadata().id(), HealthState.UP);
    }
}
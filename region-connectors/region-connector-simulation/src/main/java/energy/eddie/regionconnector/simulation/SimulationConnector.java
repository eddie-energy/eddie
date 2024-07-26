package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

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
}
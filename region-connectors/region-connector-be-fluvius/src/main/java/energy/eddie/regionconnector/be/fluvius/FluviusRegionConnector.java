package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class FluviusRegionConnector implements RegionConnector {
    @Override
    public RegionConnectorMetadata getMetadata() {
        return FluviusRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        // TODO: GH-1297
    }
}

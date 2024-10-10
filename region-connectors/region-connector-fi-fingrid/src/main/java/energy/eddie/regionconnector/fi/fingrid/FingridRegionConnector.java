package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class FingridRegionConnector implements RegionConnector {
    @Override
    public RegionConnectorMetadata getMetadata() {
        return FingridRegionConnectorMetadata.INSTANCE;
    }

    @Override
    public void terminatePermission(String permissionId) {
        // TODO: GH-1151
    }
}

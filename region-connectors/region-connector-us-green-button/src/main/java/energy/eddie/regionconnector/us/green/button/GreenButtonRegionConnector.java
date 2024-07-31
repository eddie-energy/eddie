package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class GreenButtonRegionConnector implements RegionConnector {
    @Override
    public RegionConnectorMetadata getMetadata() {
        return GreenButtonRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        // implement termination
    }
}

package energy.eddie.regionconnector.cds;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

@Component
public class CdsRegionConnector implements RegionConnector {
   private final CdsRegionConnectorMetadata metadata;

    public CdsRegionConnector(CdsRegionConnectorMetadata metadata) {this.metadata = metadata;}

    @Override
    public RegionConnectorMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void terminatePermission(String permissionId) {
        // No-Op
    }
}

package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.be.fluvius.service.TerminationService;
import org.springframework.stereotype.Component;

@Component
public class FluviusRegionConnector implements RegionConnector {
    private final TerminationService terminationService;

    public FluviusRegionConnector(TerminationService terminationService) {this.terminationService = terminationService;}

    @Override
    public RegionConnectorMetadata getMetadata() {
        return FluviusRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        terminationService.terminate(permissionId);
    }
}

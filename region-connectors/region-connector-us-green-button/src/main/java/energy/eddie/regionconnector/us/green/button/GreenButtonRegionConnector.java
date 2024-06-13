package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GreenButtonRegionConnector implements RegionConnector {
    private final GreenButtonApi greenButtonApi;

    public GreenButtonRegionConnector(
            GreenButtonApi greenButtonApi
    ) {
        this.greenButtonApi = greenButtonApi;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return GreenButtonRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        // implement termination
    }

    @Override
    public Map<String, HealthState> health() {
        return greenButtonApi.health().block();
    }
}

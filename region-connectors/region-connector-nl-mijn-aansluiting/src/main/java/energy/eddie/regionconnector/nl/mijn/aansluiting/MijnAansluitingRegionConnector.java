package energy.eddie.regionconnector.nl.mijn.aansluiting;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.TerminationService;
import org.springframework.stereotype.Component;

@Component
public class MijnAansluitingRegionConnector implements RegionConnector {
    private final TerminationService terminationService;

    public MijnAansluitingRegionConnector(TerminationService terminationService) {
        this.terminationService = terminationService;
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return MijnAansluitingRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        terminationService.terminate(permissionId);
    }
}

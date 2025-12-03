package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static energy.eddie.regionconnector.de.eta.DeEtaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DeEtaSpringConfig {
}

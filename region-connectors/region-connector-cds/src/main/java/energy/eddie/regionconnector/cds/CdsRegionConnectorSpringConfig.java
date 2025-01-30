package energy.eddie.regionconnector.cds;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class CdsRegionConnectorSpringConfig {

}

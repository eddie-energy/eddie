package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static energy.eddie.regionconnector.si.moj.elektro.SiMojElektroRegionConnectorMetadata.REGION_CONNECTOR_ID;

@RegionConnector(name = REGION_CONNECTOR_ID)
@SpringBootApplication
public class SiMojElektroSpringConfig {
}

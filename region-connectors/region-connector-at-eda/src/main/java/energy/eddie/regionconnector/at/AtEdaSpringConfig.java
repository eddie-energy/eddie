package energy.eddie.regionconnector.at;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class AtEdaSpringConfig {
}
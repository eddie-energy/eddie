package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableScheduling
public class AiidaSpringConfig {
}

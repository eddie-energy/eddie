package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID;


@SpringBootApplication
@EnableScheduling
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DatadisSpringConfig {

}

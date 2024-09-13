package energy.eddie.regionconnector.us;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import static energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata.REGION_CONNECTOR_ID;

@EnableScheduling
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableConfigurationProperties(GreenButtonConfiguration.class)
public class GreenButtonSpringConfig {
}

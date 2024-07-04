package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import static energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata.REGION_CONNECTOR_ID;

@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableConfigurationProperties(FingridConfiguration.class)
@EnableScheduling
public class FingridSpringConfiguration {
}

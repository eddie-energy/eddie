package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata.REGION_CONNECTOR_ID;

/**
 * Main Spring Boot Application configuration for the German (DE) ETA Plus region connector.
 * This class serves as the entry point for the region connector module.
 */
@EnableWebMvc
@EnableScheduling
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DeEtaSpringConfig {

}

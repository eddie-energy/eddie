package energy.eddie.regionconnector.nl.mijn.aansluiting;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.REGION_CONNECTOR_ID;


@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableConfigurationProperties(MijnAansluitingConfiguration.class)
public class NlMijnAansluitingSpringConfig {

}

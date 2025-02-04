package energy.eddie.regionconnector.cds.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(value = "region-connector.cds")
@ConditionalOnProperty(value = "region-connector.cds.enabled", havingValue = "true")
public record CdsConfiguration(URI redirectUrl) {
}

package energy.eddie.outbound.rest;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RestOutboundConnectorConfiguration.class)
public class RestOutboundBeanConfig {
}

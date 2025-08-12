package energy.eddie.outbound.metric;

import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MetricOutboundConnectorConfiguration.class)
public class MetricOutboundConnectorConfig {

}

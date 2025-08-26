package energy.eddie.outbound.metric;

import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MetricOutboundConnectorConfiguration.class)
public class MetricOutboundConnectorConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}

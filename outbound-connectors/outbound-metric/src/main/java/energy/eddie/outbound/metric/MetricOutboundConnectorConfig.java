package energy.eddie.outbound.metric;

import energy.eddie.outbound.metric.config.MetricOutboundConnectorConfiguration;
import energy.eddie.outbound.metric.connectors.AgnosticConnector;
import energy.eddie.outbound.metric.repositories.PermissionRequestMetricsRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestStatusDurationRepository;
import energy.eddie.outbound.metric.repositories.PermissionRequestTimestampRepository;
import energy.eddie.outbound.metric.service.PermissionRequestMetricsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MetricOutboundConnectorConfiguration.class)
public class MetricOutboundConnectorConfig {

    @Bean
    public PermissionRequestMetricsService permissionRequestMetricsService(
            AgnosticConnector agnosticConnector, PermissionRequestMetricsRepository metricsRepository,
            PermissionRequestStatusDurationRepository statusDurationRepository,
            PermissionRequestTimestampRepository timestampRepository
    ) {
        return new PermissionRequestMetricsService(agnosticConnector, metricsRepository, statusDurationRepository,
                timestampRepository);
    }
}

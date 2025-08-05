package energy.eddie.outbound.metric.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbound-connector.metric")
public record MetricOutboundConnectorConfiguration(String reportEndpoint, int intervalMinutes) {
}

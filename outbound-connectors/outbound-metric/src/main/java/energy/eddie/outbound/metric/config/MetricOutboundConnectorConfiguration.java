package energy.eddie.outbound.metric.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "outbound-connector.metric")
public record MetricOutboundConnectorConfiguration(String reportEndpoint, Duration interval) {
}

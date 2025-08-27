package energy.eddie.outbound.metric.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "outbound-connector.metric")
public record MetricOutboundConnectorConfiguration(String eddieId, URI endpoint) {
}

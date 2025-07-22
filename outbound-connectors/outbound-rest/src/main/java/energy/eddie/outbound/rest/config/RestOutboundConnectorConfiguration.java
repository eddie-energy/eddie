package energy.eddie.outbound.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "outbound-connector.rest")
public record RestOutboundConnectorConfiguration(Duration retentionTime) {
}

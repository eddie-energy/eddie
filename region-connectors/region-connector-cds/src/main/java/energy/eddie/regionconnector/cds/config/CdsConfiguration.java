package energy.eddie.regionconnector.cds.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(value = "region-connector.cds")
@ConditionalOnProperty(value = "region-connector.cds.enabled", havingValue = "true")
public record CdsConfiguration(@NotNull URI redirectUrl, @NotNull String clientName) {
}

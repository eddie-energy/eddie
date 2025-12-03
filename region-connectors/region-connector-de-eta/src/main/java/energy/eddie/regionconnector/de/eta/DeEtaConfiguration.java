package energy.eddie.regionconnector.de.eta;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(value = "region-connector.de-eta")
@ConditionalOnProperty(value = "region-connector.de-eta.enabled", havingValue = "true")
public record DeEtaConfiguration(@NotNull URI redirectUrl, @NotNull String clientName) {
}

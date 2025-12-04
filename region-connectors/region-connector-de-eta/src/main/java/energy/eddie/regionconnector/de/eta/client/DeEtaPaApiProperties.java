package energy.eddie.regionconnector.de.eta.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "region-connector.de-eta.pa")
public record DeEtaPaApiProperties(
        @NotBlank String paUrl,
        int connectTimeoutMs,
        int responseTimeoutMs,
        int maxRetries,
        int initialBackoffMs
) {
    public DeEtaPaApiProperties {
        if (connectTimeoutMs <= 0) connectTimeoutMs = 5000;
        if (responseTimeoutMs <= 0) responseTimeoutMs = 15000;
        if (maxRetries < 0) maxRetries = 3;
        if (initialBackoffMs <= 0) initialBackoffMs = 200;
    }
}

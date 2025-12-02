package energy.eddie.regionconnector.de.eta.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * OAuth configuration for DE-ETA region connector.
 * Bound to properties with prefix: {@code region-connector.de-eta.oauth}
 */
@Validated
@ConfigurationProperties(prefix = "region-connector.de-eta.oauth")
public record DeEtaOAuthProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotNull String authorizeUrl,
        @NotBlank String tokenUrl,
        @NotBlank String redirectUri,
        @NotBlank String scope,
        boolean enabled
) {
    public DeEtaOAuthProperties {
        if (authorizeUrl == null || authorizeUrl.isBlank()) {
            authorizeUrl = "https://eta-plus.com/oauth/authorize";
        }
    }
}

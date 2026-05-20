package energy.eddie.regionconnector.de.eta.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the German (DE) ETA Plus region connector.
 */
@Validated
@ConfigurationProperties("region-connector.de.eta")
public record DeEtaPlusConfiguration(
                String eligiblePartyId,
                @NotBlank @DefaultValue("https://int.eta-plus.com/api") String apiBaseUrl,
                @NotBlank String apiClientId,
                @NotBlank String apiClientSecret,
                @DefaultValue("/meters/historical") String meteredDataEndpoint,
                @DefaultValue("/meters/accounting-point") String accountingPointEndpoint,
                @DefaultValue("/v1/permissions/{id}") String permissionCheckEndpoint,
                @Positive @DefaultValue("30") int responseTimeoutSeconds,
                @PositiveOrZero @DefaultValue("3") int retryMaxAttempts,
                @PositiveOrZero @DefaultValue("2") int retryInitialBackoffSeconds,
                @DefaultValue("true") boolean sslEnabled,
                @DefaultValue("false") boolean sslTrustAll,
                AuthConfig auth,
                ApiConfig api) {

        @AssertTrue(message = "SSL is enabled but the API base URL uses HTTP. Either use an HTTPS URL or disable SSL.")
        private boolean isSslConsistentWithUrl() {
                return apiBaseUrl == null || sslEnabled == apiBaseUrl.startsWith("https");
        }

        public record AuthConfig(
                        String clientId,
                        String clientSecret,
                        String tokenUrl,
                        String authorizationUrl,
                        String redirectUri,
                        String scope) {
        }

        public record ApiConfig(ClientConfig client) {
                public record ClientConfig(String id, String secret) {
                }
        }
}

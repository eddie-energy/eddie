package energy.eddie.regionconnector.de.eta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the German (DE) ETA Plus region connector.
 */
@ConfigurationProperties("region-connector.de.eta")
public record DeEtaPlusConfiguration(
                String eligiblePartyId,
                String apiBaseUrl,
                AuthConfig auth,
                ApiConfig api) {

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

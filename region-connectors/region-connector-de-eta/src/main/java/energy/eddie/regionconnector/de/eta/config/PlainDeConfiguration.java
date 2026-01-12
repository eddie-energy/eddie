package energy.eddie.regionconnector.de.eta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the German (DE) ETA Plus region connector.
 * Spring infers property names from the constructor parameter names.
 */
@ConfigurationProperties("region-connector.de.eta")
public record DeEtaPlusConfiguration(
        String eligiblePartyId,
        @DefaultValue("https://api.eta-plus.de") String apiBaseUrl,
        String apiClientId,
        String apiClientSecret
) {}
}

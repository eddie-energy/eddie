package energy.eddie.regionconnector.de.eta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the German (DE) ETA Plus region connector.
 * Spring infers property names from the constructor parameter names.
 */
@ConfigurationProperties("region-connector.de.eta")
public class PlainDeConfiguration {
    private final String eligiblePartyId;
    private final String apiBaseUrl;
    private final String apiClientId;
    private final String apiClientSecret;

    public PlainDeConfiguration(
            String eligiblePartyId,
            String apiBaseUrl,
            String apiClientId,
            String apiClientSecret
    ) {
        this.eligiblePartyId = eligiblePartyId;
        this.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl : "https://api.eta-plus.de";
        this.apiClientId = apiClientId;
        this.apiClientSecret = apiClientSecret;
    }

    public String eligiblePartyId() {
        return eligiblePartyId;
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    public String apiClientId() {
        return apiClientId;
    }

    public String apiClientSecret() {
        return apiClientSecret;
    }
}

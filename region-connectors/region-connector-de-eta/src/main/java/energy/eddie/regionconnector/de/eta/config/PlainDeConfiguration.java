package energy.eddie.regionconnector.de.eta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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

    private final String meteredDataEndpoint;
    private final String permissionCheckEndpoint;

    public PlainDeConfiguration(
            String eligiblePartyId,
            @DefaultValue("https://api.eta-plus.de") String apiBaseUrl,
            String apiClientId,
            String apiClientSecret,
            @DefaultValue("/api/v1/metered-data") String meteredDataEndpoint,
            @DefaultValue("/api/v1/permissions/{id}") String permissionCheckEndpoint
    ) {
        this.eligiblePartyId = eligiblePartyId;
        this.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl : "https://api.eta-plus.de";
        this.apiClientId = apiClientId;
        this.apiClientSecret = apiClientSecret;
        this.meteredDataEndpoint = meteredDataEndpoint;
        this.permissionCheckEndpoint = permissionCheckEndpoint;
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

    public String meteredDataEndpoint() {
        return meteredDataEndpoint;
    }

    public String permissionCheckEndpoint() {
        return permissionCheckEndpoint;
    }
}
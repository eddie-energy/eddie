package energy.eddie.regionconnector.de.eta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the German (DE) ETA Plus region connector.
 * Spring infers property names from the constructor parameter names.
 */
@ConfigurationProperties("region-connector.de.eta")
public class PlainDeConfiguration {
    private static final String DEFAULT_POLLING_CRON = "0 0 17 * * *";

    private final String eligiblePartyId;
    private final String apiBaseUrl;
    private final String apiClientId;
    private final String apiClientSecret;
    private final String pollingCronExpression;

    public PlainDeConfiguration(
            String eligiblePartyId,
            String apiBaseUrl,
            String apiClientId,
            String apiClientSecret,
            String pollingCronExpression
    ) {
        this.eligiblePartyId = eligiblePartyId;
        this.apiBaseUrl = apiBaseUrl != null ? apiBaseUrl : "https://api.eta-plus.de";
        this.apiClientId = apiClientId;
        this.apiClientSecret = apiClientSecret;
        this.pollingCronExpression = pollingCronExpression != null ? pollingCronExpression : DEFAULT_POLLING_CRON;
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

    /**
     * Returns the cron expression for polling future data.
     * Default is "0 0 17 * * *" (daily at 17:00).
     *
     * @return the polling cron expression
     */
    public String pollingCronExpression() {
        return pollingCronExpression;
    }
}

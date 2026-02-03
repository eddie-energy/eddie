package energy.eddie.regionconnector.de.eta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the German (DE) ETA Plus region connector.
 * Spring infers property names from the constructor parameter names.
 */
@ConfigurationProperties("region-connector.de.eta")
public record DeEtaPlusConfiguration(
                String eligiblePartyId,
                String mdaIdentifier,
                @DefaultValue("https://api.eta-plus.de") String apiBaseUrl,
                String apiClientId,
                String apiClientSecret,
                @DefaultValue("0 0 2 * * *") String pollingCronExpression // Default: Daily at 2 AM
) {
}

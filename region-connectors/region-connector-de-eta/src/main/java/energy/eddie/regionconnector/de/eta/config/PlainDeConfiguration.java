package energy.eddie.regionconnector.de.eta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Plain implementation of DeConfiguration using Spring's value annotations.
 */
@Configuration
@SuppressWarnings("NullAway") // Fields are injected by Spring
public class PlainDeConfiguration implements DeConfiguration {

    @Value("${" + ELIGIBLE_PARTY_ID_KEY + "}")
    @SuppressWarnings("unused")
    private String eligiblePartyId;

    @Value("${" + API_BASE_URL_KEY + ":https://api.eta-plus.de}")
    @SuppressWarnings("unused")
    private String apiBaseUrl;

    @Value("${" + API_CLIENT_ID_KEY + "}")
    @SuppressWarnings("unused")
    private String apiClientId;

    @Value("${" + API_CLIENT_SECRET_KEY + "}")
    @SuppressWarnings("unused")
    private String apiClientSecret;

    @Override
    public String eligiblePartyId() {
        return eligiblePartyId;
    }

    @Override
    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    @Override
    public String apiClientId() {
        return apiClientId;
    }

    @Override
    public String apiClientSecret() {
        return apiClientSecret;
    }
}

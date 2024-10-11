package energy.eddie.regionconnector.be.fluvius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("region-connector.be.fluvius.oauth")
public class FluviusOAuthConfiguration {
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String tenantId;
    private final String scope;

    public FluviusOAuthConfiguration(
            String tokenUrl,
            String clientId,
            String clientSecret,
            String tenantId,
            String scope
    ) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.scope = scope;
    }

    public String tokenUrl() {
        return tokenUrl;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public String tenantId() {
        return tenantId;
    }

    public String scope() {
        return scope;
    }
}

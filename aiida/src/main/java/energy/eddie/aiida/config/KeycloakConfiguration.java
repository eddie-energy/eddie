package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.keycloak")
public class KeycloakConfiguration {
    private final String accountUri;
    private final String endSessionUri;

    public KeycloakConfiguration(String accountUri, String endSessionUri) {
        this.accountUri = accountUri;
        this.endSessionUri = endSessionUri;
    }

    public String accountUri() {
        return accountUri;
    }

    public String endSessionUri() {
        return endSessionUri;
    }
}

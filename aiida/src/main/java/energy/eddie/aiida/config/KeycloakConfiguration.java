package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.keycloak")
public class KeycloakConfiguration {
    private final String accountUri;

    public KeycloakConfiguration(String accountUri) {
        this.accountUri = accountUri;
    }

    public String accountUri() {
        return accountUri;
    }
}

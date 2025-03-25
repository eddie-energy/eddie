package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.keycloak")
public record KeycloakConfiguration(
        String accountUri,
        String endSessionUri
) { }

package energy.eddie.exampleappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "example-app.mqtt")
public record ExampleAppMqttConfig(
    String serverUri,
    String username,
    String password,
    String clientId
) {}

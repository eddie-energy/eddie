package energy.eddie.exampleappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "example-app.eddie")
public record ExampleAppEddieConfig(
    String id,
    String publicUrl
) {}

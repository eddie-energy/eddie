package energy.eddie.exampleappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "example-app")
public record ExampleAppConfig(
        int rtdLifetimeWindowMinutes
) {}

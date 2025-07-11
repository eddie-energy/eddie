package energy.eddie.exampleappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "example-app.kafka")
public record ExampleAppKafkaConfig(
    String messageFormat,
    String permissionCimVersion,
    String validatedHistoricalDataCimVersion
) {}

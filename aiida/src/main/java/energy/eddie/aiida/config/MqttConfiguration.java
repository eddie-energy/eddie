package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.mqtt")
public record MqttConfiguration(
        String adminUsername,
        String adminPassword,
        String internalHost,
        String externalHost
) { }

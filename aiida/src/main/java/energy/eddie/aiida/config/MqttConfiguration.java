package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.mqtt")
public record MqttConfiguration(
        String adminUsername,
        String adminPassword,
        String internalHost,
        String externalHost
) { }

// TODO: rename to MqttConfiguration and store external host in db
package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.mqtt")
public class MqttConfiguration {
    private final String host;

    public MqttConfiguration(String host) {
        this.host = host;
    }

    public String host() {
        return host;
    }
}

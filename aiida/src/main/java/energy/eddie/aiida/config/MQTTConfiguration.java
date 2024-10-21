package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.mqtt")
public class MQTTConfiguration {
    private final String host;

    public MQTTConfiguration(String host) {
        this.host = host;
    }

    public String host() {
        return host;
    }
}

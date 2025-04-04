package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.agent")
public class MonitoringAgentConfiguration {
    private final String host;

    public MonitoringAgentConfiguration(String host, String token) {
        this.host = host;
    }

    public String host() {
        return host;
    }
}
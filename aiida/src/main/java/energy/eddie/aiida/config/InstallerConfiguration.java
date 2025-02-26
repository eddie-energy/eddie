package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.installer")
public class InstallerConfiguration {
    private final String host;
    private final String token;

    public InstallerConfiguration(String host, String token) {
        this.host = host;
        this.token = token;
    }

    public String host() {
        return host;
    }

    public String token() {
        return token;
    }
}

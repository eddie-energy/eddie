package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.installer")
public record InstallerConfiguration(
        String host,
        String token
) { }

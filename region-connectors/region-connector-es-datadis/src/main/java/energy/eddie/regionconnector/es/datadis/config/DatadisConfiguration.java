package energy.eddie.regionconnector.es.datadis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "region-connector.es.datadis")
public record DatadisConfiguration(
        String username,
        String password,
        @DefaultValue("https://datadis.es") String basepath
) {}
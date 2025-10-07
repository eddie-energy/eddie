package energy.eddie.aiida.config.datasource.it;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aiida.data-source.it.sinapsi-alfa")
public record SinapsiAlfaConfiguration(
        String mqttHost,
        String mqttUsername,
        String mqttPassword
) {
    public static final String TOPIC_PREFIX = "/";
    public static final String TOPIC_INFIX = "/iomtsgdata/";
    public static final String TOPIC_SUFFIX = "/";
}

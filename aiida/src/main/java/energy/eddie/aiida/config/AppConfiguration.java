package energy.eddie.aiida.config;

import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        InstallerConfiguration.class,
        KeycloakConfiguration.class,
        MqttConfiguration.class,
        SinapsiAlfaConfig.class
})
public class AppConfiguration { }

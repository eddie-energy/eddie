package energy.eddie.aiida;

import energy.eddie.aiida.config.InstallerConfiguration;
import energy.eddie.aiida.config.KeycloakConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        CleanupConfiguration.class,
        InstallerConfiguration.class,
        KeycloakConfiguration.class,
        MqttConfiguration.class,
        SinapsiAlfaConfiguration.class
})
public class AiidaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiidaApplication.class, args);
    }
}

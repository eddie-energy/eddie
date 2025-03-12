package energy.eddie.aiida;

import energy.eddie.aiida.config.InstallerConfiguration;
import energy.eddie.aiida.config.KeycloakConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({InstallerConfiguration.class, KeycloakConfiguration.class, MqttConfiguration.class})
public class AiidaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiidaApplication.class, args);
    }

}

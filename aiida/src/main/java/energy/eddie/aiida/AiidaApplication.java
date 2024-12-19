package energy.eddie.aiida;

import energy.eddie.aiida.config.KeycloakConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakConfiguration.class)
public class AiidaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiidaApplication.class, args);
    }

}

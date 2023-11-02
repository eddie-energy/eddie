package energy.eddie.core;

import energy.eddie.core.dataneeds.DataNeedsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataNeedsConfig.class)
public class CoreSpringConfig {
    public static void main(String[] args) {
        SpringApplication.run(CoreSpringConfig.class, args);
    }
}

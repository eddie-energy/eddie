package energy.eddie.core;

import energy.eddie.core.dataneeds.DataNeedsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataNeedsService.DataNeedsConfig.class)
public class CoreSpringConfig {
    public static void main(String[] args) {
        SpringApplication.run(CoreSpringConfig.class, args);
    }
}

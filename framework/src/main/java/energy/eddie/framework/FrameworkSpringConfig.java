package energy.eddie.framework;

import energy.eddie.framework.dataneeds.DataNeedsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataNeedsService.DataNeedsConfig.class)
public class FrameworkSpringConfig {
    public static void main(String[] args) {
        SpringApplication.run(FrameworkSpringConfig.class, args);
    }
}

package energy.eddie.framework;

import energy.eddie.framework.dataneeds.DataNeedsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DataNeedsConfig.class)
public class FrameworkSpringConfig {

    public static void main(String[] args) {
        SpringApplication.run(FrameworkSpringConfig.class, args);
    }
}

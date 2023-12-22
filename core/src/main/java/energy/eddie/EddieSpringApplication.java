package energy.eddie;

import energy.eddie.core.CoreSpringConfig;
import org.springframework.boot.SpringApplication;

public class EddieSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreSpringConfig.class, args);
    }
}

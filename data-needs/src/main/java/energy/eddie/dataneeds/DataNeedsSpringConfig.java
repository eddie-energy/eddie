package energy.eddie.dataneeds;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Clock;

@Configuration
@EnableWebMvc
@SpringBootApplication
public class DataNeedsSpringConfig {
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Clock needed by e.g. {@link energy.eddie.dataneeds.validation.duration.IsValidRelativeDurationValidator}.
     *
     * @return UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

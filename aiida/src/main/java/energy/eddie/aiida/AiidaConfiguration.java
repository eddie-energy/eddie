package energy.eddie.aiida;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiidaConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

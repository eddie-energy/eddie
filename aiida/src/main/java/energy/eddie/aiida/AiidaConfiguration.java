package energy.eddie.aiida;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiidaConfiguration {
    /**
     * @return Clock instance that should be used for timestamps.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

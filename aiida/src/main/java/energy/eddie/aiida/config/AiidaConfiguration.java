package energy.eddie.aiida.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class AiidaConfiguration {
    /**
     * Configures and returns an ObjectMapper bean that should be used for (de-)serializing POJOs to JSON.
     * The ObjectMapperSingleton can also be used by classes that cannot use constructor injection
     * using the @Autowired annotation and will return the same instance.
     *
     * @return ObjectMapper instance configured to fit the AIIDA project.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Hibernate6Module module = new Hibernate6Module();
        // Jackson should automatically query any lazy loaded fields before serialization
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        objectMapper.registerModule(module);

        return objectMapper;
    }

    /**
     * Returns a clock instance that should be used for timestamps (e.g. when a permission is revoked).
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

package energy.eddie.aiida.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

@Configuration
@EnableScheduling
public class AiidaConfiguration {
    public static final ZoneId AIIDA_ZONE_ID = ZoneId.of("Etc/UTC");

    /**
     * Configures and returns an ObjectMapper bean that should be used for (de-)serializing POJOs to JSON. The
     * ObjectMapperSingleton can also be used by classes that cannot use constructor injection using the @Autowired
     * annotation and will return the same instance.
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
        // needed so that JsonTypeInformation for data need is deserialized
        module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        objectMapper.registerModule(module);

        var jtm = new JavaTimeModule();
        objectMapper.registerModule(jtm);
        // setting this to false means timestamps are formatted according to ISO and not formatted in epoch millis any more
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper;
    }

    /**
     * Returns a clock instance that should be used for timestamps (e.g. when a permission is revoked).
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * {@link WebClient} used for handshake with EDDIE.
     */
    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    // Spring's TaskScheduler only returns a ScheduledFuture<?>, so we have to use wildcards
    @SuppressWarnings("java:S1452")
    public ConcurrentMap<String, ScheduledFuture<?>> permissionFutures() {
        return new ConcurrentHashMap<>();
    }
}

package energy.eddie.aiida.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.streamers.AiidaStreamer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

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


    @SuppressWarnings("NullAway.Init")
    @Value("${aiida.streamer.poll_interval}")
    private Integer pollIntervalSeconds;

    /**
     * Specifies how frequent a {@link AiidaStreamer} should poll the EP framework if they have issued
     * a termination request.
     * Note that the actual polling interval may be higher, as the KafkaConsumer blocks for some time.
     * The specified interval is the delay between the completion of one full poll execution and the start of the next.
     */
    @Bean
    public Duration terminationRequestPollInterval() {
        return Duration.ofSeconds(pollIntervalSeconds);
    }
}

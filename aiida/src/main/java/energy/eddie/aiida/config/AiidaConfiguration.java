package energy.eddie.aiida.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.adapters.datasource.at.OesterreichsEnergieAdapterJson;
import energy.eddie.aiida.adapters.datasource.at.OesterreichsEnergieAdapterValueDeserializer;
import energy.eddie.aiida.adapters.datasource.fr.mode.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.mode.MicroTeleinfoV3DataFieldDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
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
    public Jackson2ObjectMapperBuilder customObjectMapper() {
        return new Jackson2ObjectMapperBuilder()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .failOnUnknownProperties(true)
                .modules(
                        new JavaTimeModule(),
                        new Hibernate6Module().enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING)
                                              .disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION),
                        new SimpleModule().addDeserializer(OesterreichsEnergieAdapterJson.AdapterValue.class,
                                                           new OesterreichsEnergieAdapterValueDeserializer(null)),
                        new SimpleModule().addDeserializer(MicroTeleinfoV3DataField.class,
                                                           new MicroTeleinfoV3DataFieldDeserializer(null))
                );
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
    public ConcurrentMap<UUID, ScheduledFuture<?>> permissionFutures() {
        return new ConcurrentHashMap<>();
    }
}

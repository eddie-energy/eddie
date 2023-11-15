package energy.eddie.regionconnector.aiida;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.EnableKafka;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static energy.eddie.regionconnector.aiida.config.AiidaConfiguration.*;

@SpringBootApplication
@EnableKafka
public class SpringConfig {
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(SpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            // Random port for this spring application, subject to change in GH-109
            ctx = app.run("--spring.config.import=", "--import.config.file=", "--server.port=${region-connector.aiida.server.port}");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AiidaConfiguration aiidaConfiguration(
            @Value("${" + KAFKA_BOOTSTRAP_SERVERS + "}") String kafkaBootstrapServers,
            @Value("${" + KAFKA_DATA_TOPIC + "}") String kafkaDataTopic,
            @Value("${" + KAFKA_STATUS_MESSAGES_TOPIC + "}") String kafkaStatusMessagesTopic,
            @Value("${" + KAFKA_TERMINATION_TOPIC_PREFIX + "}") String kafkaTerminationTopicPrefix
    ) {
        return new PlainAiidaConfiguration(kafkaBootstrapServers, kafkaDataTopic,
                kafkaStatusMessagesTopic, kafkaTerminationTopicPrefix);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringConfig.class, args);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AiidaFactory aiidaFactory(AiidaConfiguration configuration) {
        return new AiidaFactory(configuration);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RegionConnector regionConnector(
            @Value("${server.port:0}") int port,
            AiidaRegionConnectorService aiidaService) {
        return new AiidaRegionConnector(port, aiidaService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Sinks.Many<TerminationRequest> terminationRequestSink() {
        return Sinks
                .many()
                .unicast()
                .onBackpressureBuffer();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Flux<TerminationRequest> terminationRequestFlux(Sinks.Many<TerminationRequest> terminationRequestSink) {
        return terminationRequestSink.asFlux();
    }
}

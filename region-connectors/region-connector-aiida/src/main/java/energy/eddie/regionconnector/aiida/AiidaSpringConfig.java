package energy.eddie.regionconnector.aiida;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.kafka.ConnectionStatusMessageMixin;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.util.Set;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.aiida.config.AiidaConfiguration.*;

@EnableWebMvc
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableKafka
public class AiidaSpringConfig {
    @Bean
    public AiidaConfiguration aiidaConfiguration(
            @Value("${" + KAFKA_BOOTSTRAP_SERVERS + "}") String kafkaBootstrapServers,
            @Value("${" + KAFKA_DATA_TOPIC + "}") String kafkaDataTopic,
            @Value("${" + KAFKA_STATUS_MESSAGES_TOPIC + "}") String kafkaStatusMessagesTopic,
            @Value("${" + KAFKA_TERMINATION_TOPIC_PREFIX + "}") String kafkaTerminationTopicPrefix
    ) {
        return new PlainAiidaConfiguration(kafkaBootstrapServers, kafkaDataTopic,
                kafkaStatusMessagesTopic, kafkaTerminationTopicPrefix);
    }

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.addMixIn(ConnectionStatusMessage.class, ConnectionStatusMessageMixin.class);
        return mapper;
    }

    @Bean
    public Sinks.Many<TerminationRequest> terminationRequestSink() {
        return Sinks
                .many()
                .unicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Flux<TerminationRequest> terminationRequestFlux(Sinks.Many<TerminationRequest> terminationRequestSink) {
        return terminationRequestSink.asFlux();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }


    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Set<Extension<AiidaPermissionRequestInterface>> permissionRequestExtensions(
            AiidaPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(connectionStatusMessageSink)
        );
    }
}

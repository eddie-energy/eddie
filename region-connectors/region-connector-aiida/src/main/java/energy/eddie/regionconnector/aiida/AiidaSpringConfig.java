package energy.eddie.regionconnector.aiida;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.ConnectionStatusMessageMixin;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.TerminationRequest;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionEventRepository;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.aiida.services.AiidaTransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConsentMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.TransmissionScheduleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Clock;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
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
            @Value("${" + KAFKA_TERMINATION_TOPIC_PREFIX + "}") String kafkaTerminationTopicPrefix,
            @Value("${" + CUSTOMER_ID + "}") String customerId
    ) {
        return new PlainAiidaConfiguration(kafkaBootstrapServers, kafkaDataTopic,
                                           kafkaStatusMessagesTopic, kafkaTerminationTopicPrefix, customerId);
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
    public Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(@Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme));
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected via parent app context
    @Bean
    public TransmissionScheduleProvider<AiidaPermissionRequest> transmissionScheduleProvider(DataNeedsService dataNeedsService) {
        return new AiidaTransmissionScheduleProvider(dataNeedsService);
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, AiidaPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public ConnectionStatusMessageHandler<AiidaPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            Sinks.Many<ConnectionStatusMessage> messageSink,
            AiidaPermissionRequestViewRepository repository
    ) {
        // AIIDA does not populate additional info for messages
        return new ConnectionStatusMessageHandler<>(eventBus, messageSink, repository, request -> "");
    }

    @Bean
    public ConsentMarketDocumentMessageHandler<AiidaPermissionRequest> consentMarketDocumentMessageHandler(
            EventBus eventBus,
            AiidaPermissionRequestViewRepository repository,
            Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink,
            AiidaConfiguration configuration,
            CommonInformationModelConfiguration cimConfig,
            TransmissionScheduleProvider<AiidaPermissionRequest> transmissionScheduleProvider
    ) {
        return new ConsentMarketDocumentMessageHandler<>(eventBus,
                                                         repository,
                                                         consentMarketDocumentSink,
                                                         configuration.customerId(),
                                                         cimConfig,
                                                         transmissionScheduleProvider,
                                                         AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID);
    }
}

package energy.eddie.regionconnector.aiida;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.data.needs.AiidaEnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.aiida.mqtt.MqttConnectCallback;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionEventRepository;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.aiida.services.AiidaTransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConsentMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.utils.PasswordGenerator;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.*;
import static energy.eddie.regionconnector.aiida.config.AiidaConfiguration.*;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestController.PATH_HANDSHAKE_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;

@Configuration
public class AiidaBeanConfig {
    @Bean
    public AiidaConfiguration aiidaConfiguration(
            @Value("${" + CUSTOMER_ID + "}") String customerId,
            @Value("${" + BCRYPT_STRENGTH + "}") int bCryptStrength,
            @Value("${" + EDDIE_PUBLIC_URL + "}") String eddiePublicUrl,
            @Value("${" + MQTT_SERVER_URI + "}") String mqttServerUri,
            @Value("${" + MQTT_USERNAME + ":}") String mqttUsername,
            @Value("${" + MQTT_PASSWORD + ":}") String mqttPassword
    ) {
        String eddieUrl = eddiePublicUrl.endsWith("/") ? eddiePublicUrl : eddiePublicUrl + "/";
        String handshakeUrl = eddieUrl + ALL_REGION_CONNECTORS_BASE_URL_PATH + "/" + REGION_CONNECTOR_ID + PATH_HANDSHAKE_PERMISSION_REQUEST;

        if (mqttUsername != null && mqttUsername.trim().isEmpty())
            mqttUsername = null;

        if (mqttPassword != null && mqttPassword.trim().isEmpty())
            mqttPassword = null;

        return new PlainAiidaConfiguration(customerId,
                                           bCryptStrength,
                                           handshakeUrl,
                                           mqttServerUri,
                                           mqttUsername,
                                           mqttPassword);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
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
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
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
                                                         REGION_CONNECTOR_ZONE_ID);
    }

    @Bean
    public PasswordGenerator passwordGenerator() {
        return new PasswordGenerator(new SecureRandom());
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(AiidaConfiguration configuration) {
        return new BCryptPasswordEncoder(configuration.bCryptStrength());
    }

    @Bean
    public MqttConnectionOptions connectionOptions(AiidaConfiguration configuration) {
        MqttConnectionOptions connectionOptions = new MqttConnectionOptions();
        connectionOptions.setCleanStart(false);
        connectionOptions.setAutomaticReconnect(true);
        connectionOptions.setAutomaticReconnectDelay(1, 30);

        if (configuration.mqttUsername() != null)
            connectionOptions.setUserName(configuration.mqttUsername());

        String password = configuration.mqttPassword();
        if (password != null)
            connectionOptions.setPassword(password.getBytes(StandardCharsets.UTF_8));

        return connectionOptions;
    }

    /**
     * Creates a new {@link MqttAsyncClient} and initiates the connection to
     * {@link AiidaConfiguration#mqttServerUri()}.
     */
    @Bean
    public MqttAsyncClient mqttClient(
            MqttConnectionOptions connectionOptions,
            ThreadPoolTaskScheduler scheduler,
            AiidaConfiguration configuration
    ) throws MqttException {
        var client = new MqttAsyncClient(configuration.mqttServerUri(), // overridden by connectionOptions
                                         MQTT_CLIENT_ID,
                                         new MqttDefaultFilePersistence("./region-connector-aiida/mqtt-persistence"));

        // need to create manually because of circular dependency
        var connectCallback = new MqttConnectCallback(client, connectionOptions, scheduler);

        client.connect(connectionOptions, null, connectCallback);

        return client;
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(Clock clock) {
        return new DataNeedCalculationServiceImpl(
                SUPPORTED_DATA_NEEDS,
                AiidaRegionConnectorMetadata.getInstance(),
                new PermissionEndIsEnergyDataEndStrategy(REGION_CONNECTOR_ZONE_ID),
                new AiidaEnergyDataTimeframeStrategy(clock),
                List.of()
        );
    }
}

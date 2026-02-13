// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.data.needs.AiidaEnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.aiida.mqtt.callback.MqttConnectCallback;
import energy.eddie.regionconnector.aiida.mqtt.callback.MqttMessageCallback;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.AiidaMessageProcessorRegistry;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionEventRepository;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.aiida.publisher.MqttEventPublisher;
import energy.eddie.regionconnector.aiida.services.AiidaTransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import energy.eddie.regionconnector.shared.utils.PasswordGenerator;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Sinks;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.function.Supplier;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.MQTT_CLIENT_ID;
import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID;

@Configuration
@EnableConfigurationProperties(AiidaConfiguration.class)
@Import(ObjectMapperConfig.class)
public class AiidaBeanConfig {
    @Bean
    public JsonMapperBuilderCustomizer objectMapperCustomizer() {
        return builder -> builder.addModule(new JakartaXmlBindAnnotationModule());
    }

    @Bean
    public Sinks.Many<energy.eddie.cim.v1_04.rtd.RTDEnvelope> nearRealTimeDataCimV104Sink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<energy.eddie.cim.v1_12.rtd.RTDEnvelope> nearRealTimeDataCimV112Sink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<RawDataMessage> rawDataMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<AiidaConnectionStatusMessageDto> statusSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
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
            AiidaPermissionRequestViewRepository repository
    ) {
        // AIIDA does not populate additional info for messages
        return new ConnectionStatusMessageHandler<>(eventBus, repository, request -> "");
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<AiidaPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            AiidaPermissionRequestViewRepository repository,
            AiidaConfiguration configuration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            TransmissionScheduleProvider<AiidaPermissionRequest> transmissionScheduleProvider,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                            repository,
                                                            dataNeedsService,
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

        connectionOptions.setUserName(configuration.mqttUsername());

        String password = configuration.mqttPassword();
        if (!password.trim().isEmpty())
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
            MqttEventPublisher eventPublisher,
            AiidaConfiguration configuration
    ) throws MqttException {
        var client = new MqttAsyncClient(configuration.mqttServerUri(), // overridden by connectionOptions
                                         MQTT_CLIENT_ID,
                                         new MqttDefaultFilePersistence("./region-connector-aiida/mqtt-persistence"));

        // need to create manually because of circular dependency
        var connectCallback = new MqttConnectCallback(client, connectionOptions, eventPublisher, scheduler);

        client.connect(connectionOptions, null, connectCallback);

        return client;
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            DataNeedRuleSet ruleSet
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                AiidaRegionConnectorMetadata.getInstance(),
                new PermissionEndIsEnergyDataEndStrategy(),
                new AiidaEnergyDataTimeframeStrategy(),
                ruleSet
        );
    }

    @Bean
    public MqttMessageCallback mqttMessageCallback(AiidaMessageProcessorRegistry messageProcessorRegistry) {
        return new MqttMessageCallback(messageProcessorRegistry);
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(AiidaPermissionEventRepository repo) {
        return () -> repo;
    }
}

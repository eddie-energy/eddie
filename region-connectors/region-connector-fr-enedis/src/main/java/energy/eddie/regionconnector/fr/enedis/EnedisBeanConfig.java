package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.client.EnedisTokenProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrInternalPollingEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionEventRepository;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import energy.eddie.regionconnector.fr.enedis.services.PollingService;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

@EnableConfigurationProperties(EnedisConfiguration.class)
@Configuration
public class EnedisBeanConfig {
    @Bean
    public EnedisTokenProvider enedisTokenProvider(EnedisConfiguration config, WebClient webClient) {
        return new EnedisTokenProvider(config, webClient);
    }

    @Bean
    public WebClient webClient(EnedisConfiguration configuration) {
        return WebClient.create(configuration.basePath());
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, FrPermissionEventRepository permissionEventRepository) {
        return new Outbox(eventBus, permissionEventRepository);
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<FrEnedisPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            FrPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            EnedisConfiguration config,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                repository,
                dataNeedsService,
                config.clientId(),
                cimConfig,
                pr -> null,
                ZONE_ID_FR
        );
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            DataNeedRuleSet ruleSet
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                EnedisRegionConnectorMetadata.getInstance(),
                ruleSet
        );
    }

    @Bean
    public ConnectionStatusMessageHandler<FrEnedisPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            FrPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "");
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(
            ObjectMapper objectMapper,
            EnergyDataStreams streams
    ) {
        return new JsonRawDataProvider(
                REGION_CONNECTOR_ID,
                objectMapper,
                streams.getValidatedHistoricalData(),
                streams.getAccountingPointData()
        );
    }

    @Bean
    public CommonFutureDataService<FrEnedisPermissionRequest> commonFutureDataService(
            PollingService pollingService,
            FrPermissionRequestRepository repository,
            @Value("${region-connector.fr.enedis.polling:0 0 17 * * *}") String cronExpr,
            EnedisRegionConnector connector,
            TaskScheduler taskScheduler,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ) {
        return new CommonFutureDataService<>(
                pollingService,
                repository,
                cronExpr,
                connector.getMetadata(),
                taskScheduler,
                dataNeedCalculationService
        );
    }

    @Bean
    FulfillmentService fulfillmentService(Outbox outbox) {
        return new FulfillmentService(outbox, FrSimpleEvent::new);
    }

    @Bean
    MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService,
            Outbox outbox
    ) {
        return new MeterReadingPermissionUpdateAndFulfillmentService(
                fulfillmentService,
                (pr, meterReading) -> outbox.commit(new FrInternalPollingEvent(pr.permissionId(), meterReading))
        );
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(FrPermissionEventRepository repo) {
        return () -> repo;
    }
}

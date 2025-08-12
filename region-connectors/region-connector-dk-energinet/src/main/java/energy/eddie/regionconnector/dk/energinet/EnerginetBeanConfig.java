package energy.eddie.regionconnector.dk.energinet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PlainEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalPollingEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionEventRepository;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.SeriesPeriodBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.TimeSeriesBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.services.AccountingPointDetailsService;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
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
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY;

@Configuration
public class EnerginetBeanConfig {
    @Bean
    public EnerginetConfiguration energinetConfiguration(
            @Value("${" + ENERGINET_CUSTOMER_BASE_PATH_KEY + "}") String customerBasePath
    ) {
        return new PlainEnerginetConfiguration(customerBasePath);
    }

    @Bean
    public Flux<IdentifiableApiResponse> identifiableMeterReadingFlux(PollingService pollingService) {
        return pollingService.identifiableMeterReadings();
    }

    @Bean
    public Flux<IdentifiableAccountingPointDetails> identifiableAccountingPointDetailsFlux(AccountingPointDetailsService accountingPointDetailsService) {
        return accountingPointDetailsService.identifiableMeteringPointDetailsFlux();
    }

    @Bean
    public ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration commonInformationModelConfiguration
    ) {
        return new ValidatedHistoricalDataMarketDocumentBuilderFactory(
                commonInformationModelConfiguration,
                new TimeSeriesBuilderFactory(new SeriesPeriodBuilderFactory())
        );
    }

    @Bean
    public FulfillmentService fulfillmentService(Outbox outbox) {
        return new FulfillmentService(outbox, DkSimpleEvent::new);
    }

    @Bean
    public MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService,
            Outbox outbox
    ) {
        return new MeterReadingPermissionUpdateAndFulfillmentService(
                fulfillmentService,
                (reading, end) -> outbox.commit(new DkInternalPollingEvent(reading.permissionId(), end))
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .registerModule(new Jdk8Module());
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, DkPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public ConnectionStatusMessageHandler<DkEnerginetPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            DkPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "");
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<DkEnerginetPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            DkPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                            repository,
                                                            dataNeedsService,
                                                            cimConfig.eligiblePartyFallbackId(),
                                                            cimConfig,
                                                            pr -> Granularity.P1D.toString(),
                                                            DK_ZONE_ID);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                EnerginetRegionConnectorMetadata.getInstance()
        );
    }

    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(
            @Qualifier("objectMapper") ObjectMapper objectMapper,
            Flux<IdentifiableApiResponse> identifiableApiResponseFlux,
            Flux<IdentifiableAccountingPointDetails> accountingPointDetailsFlux
    ) {
        return new JsonRawDataProvider(
                REGION_CONNECTOR_ID,
                objectMapper,
                identifiableApiResponseFlux,
                accountingPointDetailsFlux
        );
    }

    @Bean
    public CommonFutureDataService<DkEnerginetPermissionRequest> commonFutureDataService(
            PollingService pollingService,
            DkPermissionRequestRepository repository,
            @Value("${region-connector.dk.energinet.polling:0 0 17 * * *}") String cronExpr,
            EnerginetRegionConnector connector,
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
    Supplier<PermissionEventRepository> permissionEventSupplier(DkPermissionEventRepository repo) {
        return () -> repo;
    }
}

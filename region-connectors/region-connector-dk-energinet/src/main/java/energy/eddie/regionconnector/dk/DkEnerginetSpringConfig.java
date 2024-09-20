package energy.eddie.regionconnector.dk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
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
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.pmd.CommonPermissionMarketDocumentProvider;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.*;
import static energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY;

@EnableWebMvc
@EnableScheduling
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DkEnerginetSpringConfig {
    @Bean
    public EnerginetConfiguration energinetConfiguration(
            @Value("${" + ENERGINET_CUSTOMER_BASE_PATH_KEY + "}") String customerBasePath
    ) {
        return new PlainEnerginetConfiguration(customerBasePath);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
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
    public Sinks.Many<PermissionEnvelope> permissionMarketDocumentSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
    }

    @Bean
    public ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory(
            CommonInformationModelConfiguration commonInformationModelConfiguration
    ) {
        return new ValidatedHistoricalDataMarketDocumentBuilderFactory(
                commonInformationModelConfiguration,
                new TimeSeriesBuilderFactory(new SeriesPeriodBuilderFactory())
        );
    }

    @Bean
    public PermissionMarketDocumentProvider permissionMarketDocumentProvider(Sinks.Many<PermissionEnvelope> sink) {
        return new CommonPermissionMarketDocumentProvider(sink);
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
            Sinks.Many<ConnectionStatusMessage> messages,
            DkPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, messages, repository, pr -> "");
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<DkEnerginetPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            Sinks.Many<PermissionEnvelope> pmdSink,
            DkPermissionRequestRepository repository,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                            repository,
                                                            pmdSink,
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
                SUPPORTED_DATA_NEEDS,
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
}

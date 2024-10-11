package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.data.needs.calculation.strategies.DatadisStrategy;
import energy.eddie.regionconnector.es.datadis.health.DatadisApiHealthIndicator;
import energy.eddie.regionconnector.es.datadis.permission.events.EsInternalPollingEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionEventRepository;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
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
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.DefaultEnergyDataTimeframeStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;

import java.util.List;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.*;


@SpringBootApplication
@EnableScheduling
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DatadisSpringConfig {
    @Bean
    public DatadisConfig datadisConfig(
            @Value("${" + DatadisConfig.USERNAME_KEY + "}") String username,
            @Value("${" + DatadisConfig.PASSWORD_KEY + "}") String password,
            @Value("${" + DatadisConfig.BASE_PATH_KEY + ":https://datadis.es}") String basePath
    ) {
        return new PlainDatadisConfiguration(username, password, basePath);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }

    @Bean
    public Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableMeteringData> identifiableMeteringDataFlux(Sinks.Many<IdentifiableMeteringData> sink) {
        return sink.asFlux();
    }

    @Bean
    public Sinks.Many<IdentifiableAccountingPointData> identifiableAccountingPointDataSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux(Sinks.Many<IdentifiableAccountingPointData> sink) {
        return sink.asFlux();
    }

    @Bean
    public HttpClient httpClient(DatadisApiHealthIndicator datadisApiHealthIndicator) {
        return HttpClient.create()
                         .doOnResponse((httpClientResponse, connection) -> datadisApiHealthIndicator.up())
                         .doOnRequestError((httpClientResponse, throwable) -> datadisApiHealthIndicator.down(throwable))
                         .doOnResponseError((httpClientResponse, throwable) -> datadisApiHealthIndicator.down(throwable));
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
    }

    @Bean
    public FulfillmentService fulfillmentService(Outbox outbox) {
        return new FulfillmentService(outbox, EsSimpleEvent::new);
    }

    @Bean
    public MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService,
            Outbox outbox
    ) {
        return new MeterReadingPermissionUpdateAndFulfillmentService(
                fulfillmentService,
                (request, date) -> outbox.commit(new EsInternalPollingEvent(request.permissionId(), date))
        );
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, EsPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<EsPermissionRequest> pmdHandler(
            EventBus eventBus,
            EsPermissionRequestRepository esPermissionRequestRepository,
            DatadisConfig config,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                esPermissionRequestRepository,
                config.username(),
                cimConfig,
                pr -> null,
                ZONE_ID_SPAIN
        );
    }

    @Bean
    public ConnectionStatusMessageHandler<EsPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            EsPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, repository, EsPermissionRequest::errorMessage);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                SUPPORTED_DATA_NEEDS,
                DatadisRegionConnectorMetadata.getInstance(),
                new DatadisStrategy(),
                new DefaultEnergyDataTimeframeStrategy(DatadisRegionConnectorMetadata.getInstance()),
                List.of()
        );
    }

    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(
            ObjectMapper mapper,
            Flux<IdentifiableMeteringData> meteringDataFlux,
            Flux<IdentifiableAccountingPointData> accountingPointDataFlux
    ) {
        return new JsonRawDataProvider(
                DatadisRegionConnectorMetadata.getInstance().countryCode(),
                mapper,
                meteringDataFlux,
                accountingPointDataFlux
        );
    }
}

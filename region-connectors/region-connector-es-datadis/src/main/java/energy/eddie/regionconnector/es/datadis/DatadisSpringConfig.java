package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.ContractApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.client.*;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.data.needs.calculation.strategies.DatadisStrategy;
import energy.eddie.regionconnector.es.datadis.permission.events.EsInternalPollingEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionEventRepository;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConsentMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.*;


@EnableWebMvc
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
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create();
    }

    @Bean
    public DatadisTokenProvider datadisTokenProvider(
            DatadisConfig config,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        return new NettyDatadisTokenProvider(config, httpClient, mapper);
    }

    @Bean
    public DataApi dataApi(
            DatadisTokenProvider tokenProvider,
            ObjectMapper mapper,
            DatadisConfig config,
            HttpClient httpClient
    ) {
        return new NettyDataApiClient(httpClient, mapper, tokenProvider, config.basePath());
    }

    @Bean
    public SupplyApi supplyApi(
            DatadisTokenProvider tokenProvider,
            ObjectMapper mapper,
            DatadisConfig config,
            HttpClient httpClient
    ) {
        return new NettySupplyApiClient(httpClient, mapper, tokenProvider, config.basePath());
    }

    @Bean
    public ContractApi contractApi(
            DatadisTokenProvider tokenProvider,
            ObjectMapper mapper,
            DatadisConfig config,
            HttpClient httpClient
    ) {
        return new NettyContractApiClient(httpClient, mapper, tokenProvider, config.basePath());
    }


    @Bean
    public AuthorizationApi authorizationApi(
            HttpClient httpClient,
            ObjectMapper mapper,
            DatadisTokenProvider tokenProvider,
            DatadisConfig config
    ) {
        return new NettyAuthorizationApiClient(httpClient, mapper, tokenProvider, config.basePath());
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
    }

    @Bean
    public ConsentMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new CommonConsentMarketDocumentProvider(sink);
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
    public ConsentMarketDocumentMessageHandler<EsPermissionRequest> cmdHandler(
            EventBus eventBus,
            EsPermissionRequestRepository esPermissionRequestRepository,
            Sinks.Many<ConsentMarketDocument> sink,
            DatadisConfig config,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new ConsentMarketDocumentMessageHandler<>(
                eventBus,
                esPermissionRequestRepository,
                sink,
                config.username(),
                cimConfig,
                pr -> null,
                ZONE_ID_SPAIN
        );
    }

    @Bean
    public ConnectionStatusMessageHandler<EsPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            Sinks.Many<ConnectionStatusMessage> csm,
            EsPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                csm,
                repository,
                pr -> ""
        );
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService() {
        return new DataNeedCalculationServiceImpl(
                SUPPORTED_GRANULARITIES,
                SUPPORTED_DATA_NEEDS,
                PERIOD_EARLIEST_START,
                PERIOD_LATEST_END,
                DatadisRegionConnectorMetadata.getInstance(),
                ZONE_ID_SPAIN,
                new DatadisStrategy()
        );
    }
}

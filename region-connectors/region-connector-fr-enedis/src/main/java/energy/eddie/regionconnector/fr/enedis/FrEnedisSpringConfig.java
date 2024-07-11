package energy.eddie.regionconnector.fr.enedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.client.EnedisTokenProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrInternalPollingEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionEventRepository;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.*;
import static energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration.*;

@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableAsync
@EnableRetry
@EnableScheduling
public class FrEnedisSpringConfig {
    @Bean
    public EnedisConfiguration enedisConfiguration(
            @Value("${" + ENEDIS_CLIENT_ID_KEY + "}") String clientId,
            @Value("${" + ENEDIS_CLIENT_SECRET_KEY + "}") String clientSecret,
            @Value("${" + ENEDIS_BASE_PATH_KEY + "}") String basePath
    ) {
        return new PlainEnedisConfiguration(clientId, clientSecret, basePath);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
    }

    @Bean
    public EnedisTokenProvider enedisTokenProvider(EnedisConfiguration config, WebClient webClient) {
        return new EnedisTokenProvider(config, webClient);
    }

    @Bean
    public WebClient webClient(EnedisConfiguration configuration) {
        return WebClient.create(configuration.basePath());
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> messages() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<PermissionEnveloppe> pmdSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<IdentifiableMeterReading> identifiableMeterReadingMany() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableMeterReading> identifiableMeterReadingFlux(
            Sinks.Many<IdentifiableMeterReading> identifiableMeterReadingMany
    ) {
        return identifiableMeterReadingMany.asFlux();
    }

    @Bean
    public Sinks.Many<IdentifiableAccountingPointData> identifiableAccountingPointDataMany() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux(
            Sinks.Many<IdentifiableAccountingPointData> identifiableMeterReadingMany
    ) {
        return identifiableMeterReadingMany.asFlux();
    }

    @Bean
    public PermissionMarketDocumentProvider permissionMarketDocumentProvider(Sinks.Many<PermissionEnveloppe> sink) {
        return new CommonPermissionMarketDocumentProvider(sink);
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
            Sinks.Many<PermissionEnveloppe> sink,
            EnedisConfiguration config,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                repository,
                sink,
                config.clientId(),
                cimConfig,
                pr -> null,
                ZONE_ID_FR
        );
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService() {
        return new DataNeedCalculationServiceImpl(
                SUPPORTED_DATA_NEEDS,
                EnedisRegionConnectorMetadata.getInstance()
        );
    }

    @Bean
    public ConnectionStatusMessageHandler<FrEnedisPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            Sinks.Many<ConnectionStatusMessage> csm,
            FrPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                csm,
                repository,
                pr -> ""
        );
    }

    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(
            ObjectMapper objectMapper,
            Flux<IdentifiableMeterReading> identifiableMeterReadingFlux,
            Flux<IdentifiableAccountingPointData> accountingPointDataFlux
    ) {
        return new JsonRawDataProvider(
                REGION_CONNECTOR_ID,
                objectMapper,
                identifiableMeterReadingFlux,
                accountingPointDataFlux
        );
    }
}

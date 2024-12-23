package energy.eddie.regionconnector.be.fluvius;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.data.needs.EnergyTypePredicate;
import energy.eddie.regionconnector.be.fluvius.data.needs.FluviusEnergyTimeframeStrategy;
import energy.eddie.regionconnector.be.fluvius.data.needs.FluviusPermissionTimeframeStrategy;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionEventRepository;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientSsl;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.ZoneOffset;
import java.util.List;

import static energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Configuration
public class FluviusBeanConfig {
    @Bean
    public WebClient webClient(
            WebClient.Builder builder,
            WebClientSsl webClientSsl,
            SslBundles sslBundles,
            FluviusConfiguration config
    ) {
        return builder
                .baseUrl(config.baseUrl())
                .apply(webClientSsl.fromBundle(sslBundles.getBundle("fluvius")))
                .build();
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            RegionConnectorMetadata metadata
    ) {
        return new DataNeedCalculationServiceImpl(dataNeedsService,
                metadata,
                new FluviusPermissionTimeframeStrategy(),
                new FluviusEnergyTimeframeStrategy(metadata),
                List.of(new EnergyTypePredicate()));
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, BePermissionEventRepository bePermissionEventRepository) {
        return new Outbox(eventBus, bePermissionEventRepository);
    }

    @Bean
    public ConnectionStatusMessageHandler<FluviusPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            BePermissionRequestRepository repository,
            ObjectMapper jacksonObjectMapper
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                repository,
                pr -> "",
                pr -> jacksonObjectMapper.createObjectNode().put("shortUrlIdentifier", pr.shortUrlIdentifier())
        );
    }


    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(ObjectMapper objectMapper, IdentifiableDataStreams streams) {
        return new JsonRawDataProvider(
                REGION_CONNECTOR_ID,
                objectMapper,
                streams.getMeteringData()
        );
    }

    @Bean
    public Flux<IdentifiableMeteringData> identifiableMeteringDataFlux(Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink) {
        return identifiableMeteringDataSink.asFlux();
    }

    @Bean
    public Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<FluviusPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            BePermissionRequestRepository bePermissionRequestRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            FluviusOAuthConfiguration fluviusOAuthConfiguration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                bePermissionRequestRepository,
                dataNeedsService,
                fluviusOAuthConfiguration.clientId(),
                cimConfig,
                pr -> null,
                ZoneOffset.UTC
        );
    }
}

package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.DefaultEnergyDataTimeframeStrategy;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionEventRepository;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZoneOffset;
import java.util.List;

import static energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata.SUPPORTED_DATA_NEEDS;
import static energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata.US_ZONE_ID;

@Configuration
@Import(ObjectMapperConfig.class)
public class GreenButtonBeanConfig {
    @Bean
    public ConnectionStatusMessageHandler<UsGreenButtonPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            UsPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "");
    }

    @Bean
    public WebClient webClient(GreenButtonConfiguration greenButtonConfiguration) {
        var exchangeStrategies = ExchangeStrategies
                .builder()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().jaxb2Decoder(
                        new Jaxb2XmlDecoder(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN)
                ))
                .build();

        return WebClient.builder()
                        .baseUrl(greenButtonConfiguration.basePath())
                        .exchangeStrategies(exchangeStrategies)
                        .defaultHeader(HttpHeaders.ACCEPT, "application/atom+xml")
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + greenButtonConfiguration.apiToken())
                        .build();
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, UsPermissionEventRepository permissionEventRepository) {
        return new Outbox(eventBus, permissionEventRepository);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                GreenButtonRegionConnectorMetadata.getInstance(),
                new PermissionEndIsEnergyDataEndStrategy(),
                new DefaultEnergyDataTimeframeStrategy(GreenButtonRegionConnectorMetadata.getInstance()),
                List.of()
        );
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        return marshaller;
    }

    @Bean
    public FulfillmentService fulfillmentService(Outbox outbox) {
        return new FulfillmentService(outbox, UsSimpleEvent::new);
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<UsGreenButtonPermissionRequest> pmdHandler(
            EventBus eventBus,
            UsPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                repository,
                dataNeedsService,
                cimConfig.eligiblePartyFallbackId(),
                cimConfig,
                pr -> null,
                ZoneOffset.UTC
        );
    }
}

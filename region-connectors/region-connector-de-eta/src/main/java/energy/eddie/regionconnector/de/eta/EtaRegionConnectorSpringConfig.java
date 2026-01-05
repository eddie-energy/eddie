package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the German (DE) ETA Plus region connector.
 * This configuration class sets up beans and dependencies required by the
 * region connector.
 */
@Configuration
@EnableConfigurationProperties(DeEtaPlusConfiguration.class)
public class EtaRegionConnectorSpringConfig {

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox deEtaOutbox(EventBus eventBus, DePermissionEventRepository eventRepository) {
        return new Outbox(eventBus, eventRepository);
    }

    @Bean
    public ConnectionStatusMessageHandler<DePermissionRequest> deConnectionStatusMessageHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                repository,
                req -> req.message().orElse(null));
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<DePermissionRequest> dePermissionMarketDocumentMessageHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository,
            DataNeedsService dataNeedsService,
            DeEtaPlusConfiguration configuration,
            CommonInformationModelConfiguration cimConfig,
            TransmissionScheduleProvider<DePermissionRequest> transmissionScheduleProvider) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                repository,
                dataNeedsService,
                configuration.eligiblePartyId(),
                cimConfig,
                transmissionScheduleProvider,
                EtaRegionConnectorMetadata.DE_ZONE_ID);
    }

    @Bean
    public TransmissionScheduleProvider<DePermissionRequest> deTransmissionScheduleProvider() {
        return permissionRequest -> null; // Return null for no specific transmission schedule
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            RegionConnectorMetadata metadata,
            DataNeedRuleSet ruleSet) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, metadata, ruleSet);
    }

    /**
     * Create a WebClient for making HTTP requests to the ETA Plus API
     * 
     * @return the configured WebClient
     */
    @Bean
    public WebClient deEtaWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10 MB buffer
                .build();
    }

    /**
     * Create the fulfillment service for completing permission requests.
     *
     * @param outbox the outbox for emitting events
     * @return the fulfillment service
     */
    @Bean
    public FulfillmentService deFulfillmentService(Outbox outbox) {
        return new FulfillmentService(
                outbox,
                SimpleEvent::new
        );
    }

    /**
     * Create the meter reading update and fulfillment service.
     * This service updates the latest meter reading and fulfills permission requests
     * when all data has been received.
     *
     * @param fulfillmentService the fulfillment service
     * @param outbox            the outbox for emitting events
     * @return the meter reading update and fulfillment service
     */
    @Bean
    public MeterReadingPermissionUpdateAndFulfillmentService deMeterReadingUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService,
            Outbox outbox
    ) {
        return new MeterReadingPermissionUpdateAndFulfillmentService(
                fulfillmentService,
                (reading, end) -> outbox.commit(new LatestMeterReadingEvent(reading.permissionId(), end))
        );
    }
}


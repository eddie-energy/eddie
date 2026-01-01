package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
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
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for the German (DE) ETA Plus region connector.
 * This configuration class sets up beans and dependencies required by the region connector.
 */
@Configuration
@EnableConfigurationProperties(PlainDeConfiguration.class)
public class EtaRegionConnectorSpringConfig {

    /**
     * Create the event bus for event sourcing.
     * This bean is required for all region connectors.
     * 
     * @return the event bus implementation
     */
    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    /**
     * Create the outbox for event sourcing
     * 
     * @param eventBus the event bus for publishing events
     * @param eventRepository the permission event repository
     * @return the outbox implementation
     */
    @Bean
    public Outbox deEtaOutbox(EventBus eventBus, DePermissionEventRepository eventRepository) {
        return new Outbox(eventBus, eventRepository);
    }

    /**
     * Create the connection status message handler
     * 
     * @param eventBus the event bus
     * @param repository the permission request repository
     * @return the connection status message handler
     */
    @Bean
    public ConnectionStatusMessageHandler<DePermissionRequest> deConnectionStatusMessageHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(
            eventBus,
            repository,
            req -> req.message().orElse(null)
        );
    }

    /**
     * Create the permission market document message handler
     * 
     * @param eventBus the event bus
     * @param repository the permission request repository
     * @param dataNeedsService the data needs service
     * @param configuration the DE configuration
     * @param cimConfig the CIM configuration
     * @param transmissionScheduleProvider the transmission schedule provider
     * @return the permission market document message handler
     */
    @Bean
    public PermissionMarketDocumentMessageHandler<DePermissionRequest> dePermissionMarketDocumentMessageHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository,
            DataNeedsService dataNeedsService,
            PlainDeConfiguration configuration,
            CommonInformationModelConfiguration cimConfig,
            TransmissionScheduleProvider<DePermissionRequest> transmissionScheduleProvider
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
            eventBus,
            repository,
            dataNeedsService,
            configuration.eligiblePartyId(),
            cimConfig,
            transmissionScheduleProvider,
            EtaRegionConnectorMetadata.DE_ZONE_ID
        );
    }

    /**
     * Create a transmission schedule provider for CIM documents
     * 
     * @return the transmission schedule provider
     */
    @Bean
    public TransmissionScheduleProvider<DePermissionRequest> deTransmissionScheduleProvider() {
        return permissionRequest -> null; // Return null for no specific transmission schedule
    }

    /**
     * Create the data need calculation service
     * This service is critical for the Demo Button to determine if the region connector
     * supports a given data need.
     * 
     * @param dataNeedsService the data needs service
     * @return the data need calculation service
     */
    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                EtaRegionConnectorMetadata.getInstance()
        );
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
}


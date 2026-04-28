package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.data.needs.EtaDataNeedRuleSet;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.api.agnostic.RawDataProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

/**
 * Spring configuration for the German (DE) ETA Plus region connector.
 * This configuration class sets up beans and dependencies required by the
 * region connector.
 */
@Configuration
@EnableConfigurationProperties(DeEtaPlusConfiguration.class)
public class DeEtaBeanConfig {

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
            EtaDataNeedRuleSet dataNeedRuleSet) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, EtaRegionConnectorMetadata.getInstance(), dataNeedRuleSet);
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(ObjectMapper objectMapper, ValidatedHistoricalDataStream stream) {
        return new JsonRawDataProvider(
                EtaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                objectMapper,
                stream.validatedHistoricalData()
        );
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(DePermissionEventRepository repo) {
        return () -> repo;
    }
}

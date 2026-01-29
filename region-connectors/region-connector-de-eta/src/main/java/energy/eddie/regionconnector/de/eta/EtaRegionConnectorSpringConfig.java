package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
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
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.de.eta.service.PollingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

/**
 * Spring configuration for the German (DE) ETA Plus region connector.
 * This configuration class sets up beans and dependencies required by the region connector.
 */
@Configuration
@EnableConfigurationProperties(DeEtaPlusConfiguration.class)
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
            DeEtaPlusConfiguration configuration,
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
     * @param ruleSet the data need rule set for this region connector
     * @return the data need calculation service
     */
    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            DataNeedRuleSet ruleSet
    ) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, EtaRegionConnectorMetadata.getInstance(), ruleSet);
    }


    /**
     * Create the CommonFutureDataService for periodic polling of future data.
     * This service schedules polling for active permission requests based on the configured cron expression.
     *
     * @param pollingService the polling service for fetching data
     * @param repository the permission request repository
     * @param configuration the DE configuration containing the polling cron expression
     * @param taskScheduler the Spring task scheduler
     * @param dataNeedCalculationService the data need calculation service
     * @return the configured CommonFutureDataService
     */
    @Bean
    public CommonFutureDataService<DePermissionRequest> deCommonFutureDataService(
            PollingService pollingService,
            DePermissionRequestRepository repository,
            DeEtaPlusConfiguration configuration,
            TaskScheduler taskScheduler,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ) {
        return new CommonFutureDataService<>(
                pollingService,
                repository,
                configuration.pollingCronExpression(),
                EtaRegionConnectorMetadata.getInstance(),
                taskScheduler,
                dataNeedCalculationService
        );
    }

    /**
     * Create the ObjectMapper bean for JSON serialization/deserialization.
     * Required by RawDataProvider for serializing data to JSON.
     * 
     * @return the ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Create the RawDataProvider bean for emitting raw data messages.
     * This provider emits both validated historical data and accounting point data as raw JSON.
     * 
     * Aligns with EDDIE documentation:
     * - https://architecture.eddie.energy/framework/3-extending/region-connector/quickstart.html#accounting-point-data
     * - Uses JsonRawDataProvider to serialize both data types to JSON
     * 
     * @param objectMapper the ObjectMapper for JSON serialization
     * @param validatedHistoricalDataStream the stream for validated historical data
     * @param accountingPointDataStream the stream for accounting point data
     * @return the RawDataProvider instance
     */
    @Bean
    @OnRawDataMessagesEnabled
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public RawDataProvider rawDataProvider(
            ObjectMapper objectMapper,
            ValidatedHistoricalDataStream validatedHistoricalDataStream,
            AccountingPointDataStream accountingPointDataStream
    ) {
        return new JsonRawDataProvider(
                EtaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                objectMapper,
                validatedHistoricalDataStream.validatedHistoricalData(),
                accountingPointDataStream.accountingPointData()
        );
    }
}


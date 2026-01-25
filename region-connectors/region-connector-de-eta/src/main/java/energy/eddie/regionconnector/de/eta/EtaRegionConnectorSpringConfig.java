package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.data.needs.EtaDataNeedRuleSet;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.de.eta.providers.cim.v104.DeValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.de.eta.service.PollingService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

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
            EtaDataNeedRuleSet dataNeedRuleSet) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, EtaRegionConnectorMetadata.getInstance(), dataNeedRuleSet);
    }

    @Bean
    public FulfillmentService deFulfillmentService(Outbox outbox) {
        return new FulfillmentService(
                outbox,
                SimpleEvent::new
        );
    }

    @Bean
    public MeterReadingPermissionUpdateAndFulfillmentService deMeterReadingUpdateAndFulfillmentService(
            FulfillmentService fulfillmentService,
            Outbox outbox
    ) {
        return new MeterReadingPermissionUpdateAndFulfillmentService(
                fulfillmentService,
                (reading, end) -> outbox.commit(new LatestMeterReadingEvent(
                        reading.permissionId(),
                        end.atStartOfDay(java.time.ZoneId.of("UTC"))
                ))
        );
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
}

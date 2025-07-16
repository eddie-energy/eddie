package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionEventRepository;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientSsl;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Import(ObjectMapperConfig.class)
public class FingridBeanConfiguration {

    @Bean
    public WebClient webClient(
            WebClient.Builder builder,
            SslBundles sslBundles,
            WebClientSsl webClientSsl,
            FingridConfiguration configuration
    ) {
        return builder
                .apply(webClientSsl.fromBundle(sslBundles.getBundle("fingrid")))
                .baseUrl(configuration.apiUrl())
                .build();
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, FiPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                FingridRegionConnectorMetadata.INSTANCE
        );
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<FingridPermissionRequest> cmdMessageHandler(
            EventBus eventBus,
            FiPermissionRequestRepository fiPermissionRequestRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            FingridConfiguration fingridConfiguration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                fiPermissionRequestRepository,
                dataNeedsService,
                fingridConfiguration.organisationUser(),
                cimConfig,
                pr -> "",
                FingridRegionConnectorMetadata.ZONE_ID_FINLAND
        );
    }

    @Bean
    public ConnectionStatusMessageHandler<FingridPermissionRequest> connectionStatusMessageHandler(
            FiPermissionRequestRepository fiPermissionRequestRepository,
            EventBus eventBus
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, fiPermissionRequestRepository, pr -> "");
    }

    @Bean
    public CommonFutureDataService<FingridPermissionRequest> commonFutureDataService(
            PollingService pollingService,
            FiPermissionRequestRepository repository,
            @Value("${region-connector.fi.fingrid.polling:0 0 17 * * *}") String cronExpr,
            FingridRegionConnector connector,
            TaskScheduler taskScheduler,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ) {
        return new CommonFutureDataService<>(
                pollingService,
                repository,
                cronExpr,
                connector.getMetadata(),
                taskScheduler,
                dataNeedCalculationService
        );
    }
}

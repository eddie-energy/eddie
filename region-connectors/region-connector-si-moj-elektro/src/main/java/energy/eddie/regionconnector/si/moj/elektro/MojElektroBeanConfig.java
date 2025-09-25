package energy.eddie.regionconnector.si.moj.elektro;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.si.moj.elektro.permission.request.MojElektroPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.persistence.SiPermissionEventRepository;
import energy.eddie.regionconnector.si.moj.elektro.persistence.SiPermissionRequestRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneOffset;
import java.util.function.Supplier;

@Configuration
public class MojElektroBeanConfig {

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, SiPermissionEventRepository siPermissionEventRepository) {
        return new Outbox(eventBus, siPermissionEventRepository);
    }

    @Bean
    public ConnectionStatusMessageHandler<MojElektroPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            SiPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "");
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<MojElektroPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            SiPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                repository,
                dataNeedsService,
                cimConfig.eligiblePartyFallbackId(),
                cimConfig,
                pr -> null,
                ZoneOffset.UTC);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            RegionConnectorMetadata metadata
    ) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(SiPermissionEventRepository repo) {
        return () -> repo;
    }
}

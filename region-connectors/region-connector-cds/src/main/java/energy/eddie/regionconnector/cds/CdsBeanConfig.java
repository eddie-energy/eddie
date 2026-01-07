package energy.eddie.regionconnector.cds;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionEventRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(value = CdsConfiguration.class)
public class CdsBeanConfig {
    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, CdsPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            CdsRegionConnectorMetadata metadata,
            DataNeedRuleSet ruleSet
    ) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, metadata, ruleSet);
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(CdsPermissionEventRepository repo) {
        return () -> repo;
    }
}

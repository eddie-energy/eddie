package energy.eddie.regionconnector.de.eta;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionEventRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.openapitools.jackson.nullable.JsonNullableModule;
import reactor.core.publisher.Mono;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(value = {DeEtaConfiguration.class, energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthProperties.class})
public class DeEtaBeanConfig {
    @Bean
    public EventBus eventBus() {return new EventBusImpl();}

    @Bean
    public Outbox outbox(EventBus eventBus, DeEtaPermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder customObjectMapper() {
        return new Jackson2ObjectMapperBuilder().modules(new JsonNullableModule(), new Jdk8Module(), new JavaTimeModule());
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {return builder.build();}

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            DeEtaRegionConnectorMetadata metadata
    ) {
        return new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(DeEtaPermissionEventRepository repo) {
        return () -> repo;
    }

    // Temporary default MDA client to enable data flow until a real implementation is wired.
    // Returns placeholder payloads so downstream mapping and events can proceed.
    @Bean
    public energy.eddie.regionconnector.de.eta.client.DeEtaMdaApiClient deEtaMdaApiClient() {
        return new energy.eddie.regionconnector.de.eta.client.DeEtaMdaApiClient() {
            @Override
            public Mono<ValidatedHistoricalDataResponse> fetchValidatedHistoricalData(
                    energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest permissionRequest
            ) {
                return Mono.just(new ValidatedHistoricalDataResponse(new Object()));
            }

            @Override
            public Mono<AccountingPointDataResponse> fetchAccountingPointData(
                    energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest permissionRequest
            ) {
                return Mono.just(new AccountingPointDataResponse(new Object()));
            }
        };
    }
}

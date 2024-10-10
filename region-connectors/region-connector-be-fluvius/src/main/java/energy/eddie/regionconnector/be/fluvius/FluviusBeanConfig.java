package energy.eddie.regionconnector.be.fluvius;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.data.needs.FluviusEnergyTimeframeStrategy;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionEventRepository;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientSsl;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZoneOffset;
import java.util.List;

@Configuration
public class FluviusBeanConfig {
    @Bean
    public WebClient webClient(
            WebClient.Builder builder,
            WebClientSsl webClientSsl,
            SslBundles sslBundles,
            FluviusConfiguration config
    ) {
        return builder
                .baseUrl(config.baseUrl())
                .apply(webClientSsl.fromBundle(sslBundles.getBundle("fluvius")))
                .build();
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService) {
        return new DataNeedCalculationServiceImpl(dataNeedsService,
                                                  FluviusRegionConnectorMetadata.SUPPORTED_DATA_NEEDS,
                                                  FluviusRegionConnectorMetadata.getInstance(),
                                                  new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC),
                                                  new FluviusEnergyTimeframeStrategy(FluviusRegionConnectorMetadata.getInstance()),
                                                  List.of());
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, BePermissionEventRepository bePermissionEventRepository) {
        return new Outbox(eventBus, bePermissionEventRepository);
    }

    @Bean
    public ConnectionStatusMessageHandler<FluviusPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            BePermissionRequestRepository repository,
            ObjectMapper jacksonObjectMapper
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                repository,
                pr -> "",
                pr -> jacksonObjectMapper.createObjectNode().put("shortUrlIdentifier", pr.shortUrlIdentifier())
        );
    }
}

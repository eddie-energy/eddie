package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionEventRepository;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.pmd.CommonPermissionMarketDocumentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientSsl;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Sinks;

import java.util.List;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;

@Configuration
public class FingridBeanConfiguration {

    @Bean
    public WebClient webClient(
            WebClient.Builder builder,
            SslBundles sslBundles,
            WebClientSsl webClientSsl
    ) {
        return builder
                .apply(webClientSsl.fromBundle(sslBundles.getBundle("fingrid")))
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
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                List.of(ValidatedHistoricalDataDataNeed.class),
                FingridRegionConnectorMetadata.INSTANCE
        );
    }

    @Bean
    public PermissionMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<PermissionEnvelope> cmdSink) {
        return new CommonPermissionMarketDocumentProvider(cmdSink);
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<FingridPermissionRequest> cmdMessageHandler(
            EventBus eventBus,
            FiPermissionRequestRepository fiPermissionRequestRepository,
            Sinks.Many<PermissionEnvelope> cmdSink,
            FingridConfiguration fingridConfiguration,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                fiPermissionRequestRepository,
                cmdSink,
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
    public Sinks.Many<PermissionEnvelope> cmdSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }
}

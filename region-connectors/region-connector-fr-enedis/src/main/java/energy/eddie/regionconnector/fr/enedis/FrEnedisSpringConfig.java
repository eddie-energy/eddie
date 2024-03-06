package energy.eddie.regionconnector.fr.enedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisTokenProvider;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.ConsentMarketDocumentExtension;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Set;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration.*;

@EnableWebMvc
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableAsync
@EnableRetry
@EnableScheduling
public class FrEnedisSpringConfig {
    @Bean
    public EnedisConfiguration enedisConfiguration(
            @Value("${" + ENEDIS_CLIENT_ID_KEY + "}") String clientId,
            @Value("${" + ENEDIS_CLIENT_SECRET_KEY + "}") String clientSecret,
            @Value("${" + ENEDIS_BASE_PATH_KEY + "}") String basePath
    ) {
        return new PlainEnedisConfiguration(clientId, clientSecret, basePath);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new Jdk8Module());
    }

    @Bean
    public CommonInformationModelConfiguration commonInformationModelConfiguration(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingSchemeTypeList) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingSchemeTypeList));
    }

    @Bean
    public EnedisTokenProvider enedisTokenProvider(EnedisConfiguration config, WebClient webClient) {
        return new EnedisTokenProvider(config, webClient);
    }

    @Bean
    public WebClient webClient(EnedisConfiguration configuration) {
        return WebClient.create(configuration.basePath());
    }

    @Bean
    public EnedisApi enedisApi(EnedisTokenProvider tokenProvider, WebClient webClient) {
        return new EnedisApiClient(tokenProvider, webClient);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> messages() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsentMarketDocument> cmdSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<IdentifiableMeterReading> identifiableMeterReadingMany() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableMeterReading> identifiableMeterReadingFlux(
            Sinks.Many<IdentifiableMeterReading> identifiableMeterReadingMany
    ) {
        return identifiableMeterReadingMany.asFlux();
    }

    @Bean
    public Set<Extension<FrEnedisPermissionRequest>> extensions(PermissionRequestRepository<FrEnedisPermissionRequest> repository,
                                                                Sinks.Many<ConnectionStatusMessage> messages,
                                                                Sinks.Many<ConsentMarketDocument> cmds,
                                                                EnedisConfiguration config,
                                                                CommonInformationModelConfiguration cimConfig) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(messages),
                new ConsentMarketDocumentExtension<>(
                        cmds,
                        config.clientId(),
                        cimConfig.eligiblePartyNationalCodingScheme().value()
                )
        );
    }

    @Bean
    public PermissionRequestFactory factory(
            Set<Extension<FrEnedisPermissionRequest>> extensions,
            StateBuilderFactory stateBuilderFactory
    ) {
        return new PermissionRequestFactory(extensions, stateBuilderFactory);
    }

    @Bean
    public StateBuilderFactory stateBuilderFactory() {
        return new StateBuilderFactory();
    }

    @Bean
    public ConsentMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new CommonConsentMarketDocumentProvider(sink);
    }
}

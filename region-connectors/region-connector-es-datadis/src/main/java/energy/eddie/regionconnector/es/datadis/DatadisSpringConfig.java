package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.client.*;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.es.datadis.services.DatadisScheduler;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.ConsentMarketDocumentExtension;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;

import java.util.Set;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID;


@EnableWebMvc
@SpringBootApplication
@energy.eddie.api.agnostic.RegionConnector(name = REGION_CONNECTOR_ID)
public class DatadisSpringConfig {
    @Bean
    public DatadisConfig datadisConfig(
            @Value("${" + DatadisConfig.USERNAME_KEY + "}") String username,
            @Value("${" + DatadisConfig.PASSWORD_KEY + "}") String password) {
        return new PlainDatadisConfiguration(username, password);
    }

    @Bean
    public EsPermissionRequestRepository repository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    public DatadisEndpoints datadisEndpoints() {
        return new DatadisEndpoints();
    }

    @Bean
    public Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Flux<IdentifiableMeteringData> identifiableMeteringDataFlux(Sinks.Many<IdentifiableMeteringData> sink) {
        return sink.asFlux();
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public DatadisTokenProvider datadisTokenProvider(
            DatadisConfig config,
            DatadisEndpoints endpoints) {
        var httpClient = HttpClient.create();
        return new NettyDatadisTokenProvider(config, httpClient, endpoints);
    }

    @Bean
    public DataApi dataApi(DatadisTokenProvider tokenProvider, DatadisEndpoints endpoints) {
        var httpClient = HttpClient.create();
        return new NettyDataApiClient(httpClient, tokenProvider, endpoints);
    }

    @Bean
    public AuthorizationApi authorizationApi(DatadisTokenProvider tokenProvider, DatadisEndpoints endpoints) {
        var httpClient = HttpClient.create();
        return new NettyAuthorizationApiClient(httpClient, tokenProvider, endpoints);
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(@Value("${" + CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme));
    }

    @Bean
    public Set<Extension<EsPermissionRequest>> permissionRequestExtensions(EsPermissionRequestRepository repository,
                                                                           Sinks.Many<ConnectionStatusMessage> messages,
                                                                           Sinks.Many<ConsentMarketDocument> cmds,
                                                                           DatadisConfig config,
                                                                           CommonInformationModelConfiguration cimConfig) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(messages),
                new ConsentMarketDocumentExtension<>(
                        cmds,
                        config.username(),
                        cimConfig.eligiblePartyNationalCodingScheme().value()
                )
        );
    }

    @Bean
    public PermissionRequestService permissionRequestService(
            EsPermissionRequestRepository repository,
            PermissionRequestFactory permissionRequestFactory,
            DatadisScheduler datadisScheduler) {
        return new PermissionRequestService(repository, permissionRequestFactory, datadisScheduler);
    }

    @Bean
    public ConsentMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new CommonConsentMarketDocumentProvider(sink);
    }
}
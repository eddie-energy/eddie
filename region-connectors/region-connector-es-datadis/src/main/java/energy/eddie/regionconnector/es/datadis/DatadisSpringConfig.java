package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.client.*;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.DatadisScheduler;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
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

    /**
     * Creates the Sink which is used to publish {@link ConsumptionRecord}s.
     * The Flux of this sink allows only one subscriber!
     *
     * @return Sink for consumption records.
     */
    @Bean
    public Sinks.Many<ConsumptionRecord> consumptionRecordSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
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
    public Set<Extension<EsPermissionRequest>> permissionRequestExtensions(EsPermissionRequestRepository repository,
                                                                           Sinks.Many<ConnectionStatusMessage> messages) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(messages)
        );
    }

    @Bean
    public PermissionRequestService permissionRequestService(
            EsPermissionRequestRepository repository,
            PermissionRequestFactory permissionRequestFactory,
            DatadisScheduler datadisScheduler) {
        return new PermissionRequestService(repository, permissionRequestFactory, datadisScheduler);
    }
}
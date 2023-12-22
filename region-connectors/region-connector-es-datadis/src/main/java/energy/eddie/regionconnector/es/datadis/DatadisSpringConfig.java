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
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.DatadisScheduler;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;

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
    public DatadisScheduler datadisScheduler(DataApi dataApi, Sinks.Many<ConsumptionRecord> consumptionRecordSink) {
        return new DatadisScheduler(dataApi, consumptionRecordSink);
    }

    @Bean
    public PermissionRequestService permissionRequestService(
            EsPermissionRequestRepository repository,
            PermissionRequestFactory permissionRequestFactory,
            DatadisScheduler datadisScheduler) {
        return new PermissionRequestService(repository, permissionRequestFactory, datadisScheduler);
    }
}

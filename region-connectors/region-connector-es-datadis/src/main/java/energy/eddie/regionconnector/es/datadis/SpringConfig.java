package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
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
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;


@SpringBootApplication
public class SpringConfig {
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(SpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            ctx = app.run("--spring.config.import=", "--import.config.file=", "--server.port=${region-connector.es.datadis.server.port}");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringConfig.class, args);
    }

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
    public PermissionRequestFactory permissionRequestFactory(
            AuthorizationApi authorizationApi,
            Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink,
            EsPermissionRequestRepository repository) {
        return new PermissionRequestFactory(authorizationApi, connectionStatusMessageSink, repository);
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

    @Bean
    public RegionConnector regionConnector(Sinks.Many<ConsumptionRecord> consumptionRecordSink,
                                           Sinks.Many<ConnectionStatusMessage> statusMessageSink,
                                           PermissionRequestService permissionRequestService,
                                           @Value("${server.port:0}") int port) {
        return new DatadisRegionConnector(statusMessageSink, consumptionRecordSink, permissionRequestService, port);
    }
}

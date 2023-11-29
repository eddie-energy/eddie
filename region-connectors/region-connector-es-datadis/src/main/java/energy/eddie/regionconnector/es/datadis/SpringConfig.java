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
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
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

    @Bean
    public Sinks.Many<ConsumptionRecord> consumptionRecordSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
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
    // TODO why does @Service not create a bean?
    public PermissionRequestService service(EsPermissionRequestRepository repository) {
        return new PermissionRequestService(repository);
    }

    @Bean
    public RegionConnector regionConnector(Sinks.Many<ConsumptionRecord> consumptionRecordSink,
                                           Sinks.Many<ConnectionStatusMessage> statusMessageSink,
                                           @Value("${server.port:0}") int port) {
        return new DatadisRegionConnector(statusMessageSink, consumptionRecordSink);
    }
}

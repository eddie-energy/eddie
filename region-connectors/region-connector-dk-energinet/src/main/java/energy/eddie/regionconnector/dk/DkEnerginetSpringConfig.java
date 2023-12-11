package energy.eddie.regionconnector.dk;

import energy.eddie.api.agnostic.SpringRegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.PlainEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Sinks;

import static energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration.ENERGINET_CUSTOMER_BASE_PATH_KEY;

@Configuration
@ComponentScan
@EnableWebMvc
@SpringRegionConnector(name = "dk-energinet")
public class DkEnerginetSpringConfig {
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(DkEnerginetSpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            ctx = app.run("--spring.config.import=", "--import.config.file=", "--server.port=${region-connector.dk.energinet.server.port}");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    @Bean
    public EnerginetConfiguration energinetConfiguration(
            @Value("${" + ENERGINET_CUSTOMER_BASE_PATH_KEY + "}") String customerBasePath) {
        return new PlainEnerginetConfiguration(customerBasePath);
    }

    @Bean
    public PermissionRequestFactory permissionRequestFactory(
            DkEnerginetCustomerPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> statusMessageSink,
            EnerginetConfiguration configuration) {
        return new PermissionRequestFactory(repository, statusMessageSink, configuration);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Sinks.Many<ConsumptionRecord> consumptionRecordSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public EnerginetCustomerApi energinetCustomerApi(EnerginetConfiguration config) {
        return new EnerginetCustomerApiClient(config);
    }

    @Bean
    public DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    public RegionConnector regionConnector(
            Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink,
            EnerginetCustomerApi energinetCustomerApi,
            DkEnerginetCustomerPermissionRequestRepository repository,
            @Value("${server.port:0}") int port) {
        return new EnerginetRegionConnector(connectionStatusSink, consumptionRecordSink, energinetCustomerApi, repository, port);
    }
}

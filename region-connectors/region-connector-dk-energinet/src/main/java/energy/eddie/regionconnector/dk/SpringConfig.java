package energy.eddie.regionconnector.dk;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.ConfigEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.spring.ConfigInitializer;
import jakarta.annotation.Nullable;
import org.eclipse.microprofile.config.Config;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start(Config config) {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(SpringConfig.class)
                    .web(WebApplicationType.NONE)
                    .initializers(new ConfigInitializer(config))
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the frameworks configuration.
            ctx = app.run("--spring.config.import=", "--import.config.file=");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    @Bean
    // The config is wired via the SpringApplicationBuilder, which leads to the warning that config is not wired.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EnerginetConfiguration energinetConfiguration(Config config) {
        return new ConfigEnerginetConfiguration(config);
    }

    @Bean
    // The config is wired via the SpringApplicationBuilder, which leads to the warning that config is not wired.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EnerginetCustomerApi energinetCustomerApi(Config config) {
        return new EnerginetCustomerApiClient(energinetConfiguration(config));
    }

    @Bean
    public DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    public RegionConnector regionConnector(
            EnerginetConfiguration energinetConfiguration,
            EnerginetCustomerApi energinetCustomerApi,
            DkEnerginetCustomerPermissionRequestRepository repository
    ) {
        return new EnerginetRegionConnector(energinetConfiguration, energinetCustomerApi, repository);
    }
}

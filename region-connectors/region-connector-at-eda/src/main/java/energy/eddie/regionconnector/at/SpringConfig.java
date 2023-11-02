package energy.eddie.regionconnector.at;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.ConfigAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.ConfigPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.spring.ConfigInitializer;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import org.eclipse.microprofile.config.Config;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

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
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            ctx = app.run("--spring.config.import=", "--import.config.file=");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    @Bean
    // The config is wired via the SpringApplicationBuilder, which leads to the warning that config is not wired.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public PontonXPAdapterConfiguration pontonXPAdapterConfiguration(Config config) {
        return new ConfigPontonXPAdapterConfiguration(config);
    }

    @Bean
    // The config is wired via the SpringApplicationBuilder, which leads to the warning that config is not wired.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AtConfiguration atConfiguration(Config config) {
        return new ConfigAtConfiguration(config);
    }

    @Bean
    public AtPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    public EdaAdapter edaAdapter(PontonXPAdapterConfiguration configuration) throws JAXBException, IOException, ConnectionException {
        return new PontonXPAdapter(configuration);
    }

    @Bean
    public RegionConnector regionConnector(
            AtConfiguration atConfiguration,
            AtPermissionRequestRepository repository,
            EdaAdapter edaAdapter
    ) throws TransmissionException {
        return new EdaRegionConnector(atConfiguration, edaAdapter, repository);
    }
}

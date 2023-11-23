package energy.eddie.regionconnector.at;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import org.springframework.boot.WebApplicationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static energy.eddie.regionconnector.at.eda.config.AtConfiguration.ELIGIBLE_PARTY_ID_KEY;
import static energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration.*;

@SpringBootApplication
public class SpringConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringConfig.class);
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(SpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            ctx = app.run("--spring.config.import=", "--import.config.file=");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringConfig.class, args);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PontonXPAdapterConfiguration pontonXPAdapterConfiguration(@Value("${" + ADAPTER_ID_KEY + "}") String adapterId,
                                                                     @Value("${" + ADAPTER_VERSION_KEY + "}") String adapterVersion,
                                                                     @Value("${" + HOSTNAME_KEY + "}") String hostname,
                                                                     @Value("${" + PORT_KEY + "}") int port,
                                                                     @Value("${" + WORK_FOLDER_KEY + "}") String workFolder) {
        return new PlainPontonXPAdapterConfiguration(adapterId, adapterVersion, hostname, port, workFolder);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AtConfiguration atConfiguration(@Value("${" + ELIGIBLE_PARTY_ID_KEY + "}") String eligiblePartyId) {
        return new PlainAtConfiguration(eligiblePartyId);
    }

    @Bean
    public AtPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public EdaAdapter edaAdapter(PontonXPAdapterConfiguration configuration, Environment environment) throws JAXBException, IOException, ConnectionException {
        try {
            return new PontonXPAdapter(configuration);
        } catch (Exception exception) {
            if (environment.matchesProfiles("dev")) {
                LOGGER.warn("Using NoOp Eda Adapter, since exception occurred", exception);
                return new NoOpEdaAdapter();
            }
            throw exception;
        }
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks
                .many()
                .multicast()
                .onBackpressureBuffer();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Set<Extension<AtPermissionRequest>> permissionRequestExtensions(AtPermissionRequestRepository repository,
                                                                           Sinks.Many<ConnectionStatusMessage> messages) {
        return Set.of(
                new SavingExtension(repository),
                new MessagingExtension(messages)
        );
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

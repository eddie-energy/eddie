package energy.eddie.framework;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import energy.eddie.api.v0.ApplicationConnector;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.framework.web.JavalinApp;
import energy.eddie.framework.web.JavalinPathHandler;
import energy.eddie.framework.web.PermissionFacade;
import energy.eddie.outbound.kafka.KafkaConnector;
import io.smallrye.config.PropertiesConfigSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

public class Framework {
    private static final Logger LOGGER = LoggerFactory.getLogger(Framework.class);

    private final JavalinApp javalinApp;
    private final PermissionService permissionService;
    private final ConsumptionRecordService consumptionRecordService;
    private final Set<ApplicationConnector> applicationConnectors;

    @Inject
    public Framework(JavalinApp javalinApp,
                     PermissionService permissionService,
                     ConsumptionRecordService consumptionRecordService,
                     Set<ApplicationConnector> applicationConnectors) {
        this.javalinApp = javalinApp;
        this.permissionService = permissionService;
        this.consumptionRecordService = consumptionRecordService;
        this.applicationConnectors = applicationConnectors;
    }

    public static void main(String[] args) {
        var injector = Guice.createInjector(new Module());
        injector.getInstance(Framework.class).run(args);
    }

    public void run(String[] args) {
        LOGGER.info("Starting up EDDIE framework");
        var connectionStatusMessageStream = JdkFlowAdapter.publisherToFlowPublisher(permissionService.getConnectionStatusMessageStream());
        var consumptionRecordMessageStream = JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordService.getConsumptionRecordStream());

        applicationConnectors.forEach(applicationConnector -> {
            applicationConnector.init();
            applicationConnector.setConnectionStatusMessageStream(connectionStatusMessageStream);
            applicationConnector.setConsumptionRecordStream(consumptionRecordMessageStream);
        });
        FrameworkSpringConfig.main(args);
        javalinApp.init();
    }

    private static class Module extends AbstractModule {

        private static final String APPLICATION_PROPERTIES = "application.properties";

        @Override
        protected void configure() {
            Config config = loadExternalConfig();
            bind(Config.class).toInstance(config);

            var applicationConnectorBinder = Multibinder.newSetBinder(binder(), ApplicationConnector.class);
            applicationConnectorBinder.addBinding().to(JdbcAdapter.class);
            if (config.getOptionalValue("kafka.enabled", Boolean.class).orElse(true)) {
                applicationConnectorBinder.addBinding().toInstance(new KafkaConnector(KafkaProperties.fromConfig(config)));
            }

            var pathHandlerBinder = Multibinder.newSetBinder(binder(), JavalinPathHandler.class);
            pathHandlerBinder.addBinding().to(PermissionFacade.class);

            var regionConnectorBinder = Multibinder.newSetBinder(binder(), RegionConnector.class);
            getAllConnectors(config).forEach(rc -> regionConnectorBinder.addBinding().toInstance(rc));
        }

        private Collection<RegionConnector> getAllConnectors(Config config) {
            var allConnectors = ServiceLoader.load(RegionConnectorFactory.class).stream()
                    .map(provider -> {
                        try {
                            // instantiate all connectors and ignore those that can't be constructed
                            return Optional.of(provider.get().create(config));
                        } catch (Exception e) {
                            LOGGER.error("Could not load/create RegionConnector '{}'", provider.type().getName(), e);
                            return Optional.<RegionConnector>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            if (!allConnectors.isEmpty()) {
                var paNames = allConnectors.stream().map(RegionConnector::getMetadata).map(RegionConnectorMetadata::mdaCode).toArray();
                LOGGER.info("Found {} Connectors: {}", allConnectors.size(), paNames);
            } else {
                LOGGER.error("No Connectors found, cannot receive any data");
                throw new InitializationException("No Connectors found, cannot receive any data");
            }
            return allConnectors;
        }

        private Config loadExternalConfig() {
            try {
                ConfigProviderResolver resolver = ConfigProviderResolver.instance();
                ConfigBuilder builder = resolver
                        .getBuilder()
                        .addDefaultSources();
                URL applicationProperties = getClass().getClassLoader().getResource(APPLICATION_PROPERTIES);
                if (applicationProperties != null) {
                    builder
                            .withSources(new PropertiesConfigSource(applicationProperties, 110));
                }
                File externalApplicationProperties = new File("./" + APPLICATION_PROPERTIES);
                if (externalApplicationProperties.exists()) {
                    builder
                            .withSources(new PropertiesConfigSource(externalApplicationProperties.toURI().toURL(), 120));
                }
                return builder.build();
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    public static final class InitializationException extends RuntimeException {
        public InitializationException(String message) {
            super(message);
        }
    }

    static class ConfigurationException extends RuntimeException {
        ConfigurationException(Throwable e) {
            super(e);
        }
    }
}
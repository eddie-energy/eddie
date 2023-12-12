package energy.eddie.core;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import energy.eddie.api.v0.ApplicationConnector;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.ConsumptionRecordService;
import energy.eddie.core.web.JavalinApp;
import energy.eddie.core.web.JavalinPathHandler;
import energy.eddie.core.web.PermissionFacade;
import energy.eddie.outbound.kafka.KafkaConnector;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorFactory;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorFactory;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorFactory;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorFactory;
import energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorFactory;
import energy.eddie.regionconnector.simulation.SimulationRegionConnectorFactory;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class Core {
    private static final Logger LOGGER = LoggerFactory.getLogger(Core.class);

    private final JavalinApp javalinApp;
    private final PermissionService permissionService;
    private final ConsumptionRecordService consumptionRecordService;
    private final EddieValidatedHistoricalDataMarketDocumentService eddieValidatedHistoricalDataMarketDocumentService;
    private final Set<ApplicationConnector> applicationConnectors;

    @Inject
    public Core(JavalinApp javalinApp,
                PermissionService permissionService,
                ConsumptionRecordService consumptionRecordService,
                EddieValidatedHistoricalDataMarketDocumentService eddieValidatedHistoricalDataMarketDocumentService,
                Set<ApplicationConnector> applicationConnectors) {
        this.javalinApp = javalinApp;
        this.permissionService = permissionService;
        this.consumptionRecordService = consumptionRecordService;
        this.eddieValidatedHistoricalDataMarketDocumentService = eddieValidatedHistoricalDataMarketDocumentService;
        this.applicationConnectors = applicationConnectors;
    }

    public static void main(String[] args) {
        var injector = Guice.createInjector(new Module());
        injector.getInstance(Core.class).run(args);
    }

    public void run(String[] args) {
//        CoreSpringConfig.main(args);
        LOGGER.info("Starting up EDDIE");
        var connectionStatusMessageStream = JdkFlowAdapter.publisherToFlowPublisher(permissionService.getConnectionStatusMessageStream());
        var consumptionRecordMessageStream = JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordService.getConsumptionRecordStream());
        var eddieValidatedHistoricalDataMarketDocumentStream = JdkFlowAdapter.publisherToFlowPublisher(eddieValidatedHistoricalDataMarketDocumentService.getEddieValidatedHistoricalDataMarketDocumentStream());

        applicationConnectors.forEach(applicationConnector -> {
            applicationConnector.init();
            applicationConnector.setConnectionStatusMessageStream(connectionStatusMessageStream);
            applicationConnector.setConsumptionRecordStream(consumptionRecordMessageStream);

            if (applicationConnector instanceof energy.eddie.api.v0_82.ApplicationConnector ac) {
                ac.setEddieValidatedHistoricalDataMarketDocumentStream(eddieValidatedHistoricalDataMarketDocumentStream);
            }
        });
        javalinApp.init();
    }

    private static class Module extends AbstractModule {

        private static final String APPLICATION_PROPERTIES = "application.properties";
        private static final RegionConnectorFactory[] regionConnectorFactories = {
                new DatadisRegionConnectorFactory(),
                new EdaRegionConnectorFactory(),
                new EnerginetRegionConnectorFactory(),
                new EnedisRegionConnectorFactory(),
                new SimulationRegionConnectorFactory(),
                new AiidaRegionConnectorFactory()
        };

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
            var allConnectors = Arrays.stream(regionConnectorFactories)
                    .map(regionConnectorFactory -> {
                        try {
                            // instantiate all connectors and ignore those that can't be constructed
                            return Optional.of(regionConnectorFactory.create(config));
                        } catch (Exception e) {
                            LOGGER.error("Could not load/create RegionConnector '{}'", regionConnectorFactories.getClass().getName(), e);
                            return Optional.<RegionConnector>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            if (!allConnectors.isEmpty()) {
                var paNames = allConnectors.stream().map(RegionConnector::getMetadata).map(RegionConnectorMetadata::id).toArray();
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
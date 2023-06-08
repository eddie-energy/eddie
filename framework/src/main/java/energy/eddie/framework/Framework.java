package energy.eddie.framework;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.framework.web.JavalinApp;
import energy.eddie.framework.web.JavalinPathHandler;
import energy.eddie.framework.web.PermissionFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;

import java.util.Collection;
import java.util.ServiceLoader;

public class Framework implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Framework.class);

    @Inject
    private JdbcAdapter jdbcAdapter;
    @Inject
    private JavalinApp javalinApp;
    @Inject
    private ConsentService consentService;
    @Inject
    private ConsumptionRecordService consumptionRecordService;

    private static class Module extends AbstractModule {

        @Override
        protected void configure() {
            bind(JavalinApp.class).toInstance(new JavalinApp());
            bind(Framework.class).toInstance(new Framework());
            bind(JdbcAdapter.class).toInstance(new JdbcAdapter(Env.JDBC_URL.get(), Env.JDBC_USER.get(), Env.JDBC_PASSWORD.get()));

            var pathHandlerBinder = Multibinder.newSetBinder(binder(), JavalinPathHandler.class);
            pathHandlerBinder.addBinding().to(PermissionFacade.class);

            var regionConnectorBinder = Multibinder.newSetBinder(binder(), RegionConnector.class);
            getAllConnectors().forEach(rc -> regionConnectorBinder.addBinding().toInstance(rc));
        }

        private Collection<RegionConnector> getAllConnectors() {
            var allConnectors = ServiceLoader.load(RegionConnector.class).stream()
                    .map(ServiceLoader.Provider::get)
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
    }

    public static void main(String[] args) {
        var injector = Guice.createInjector(new Module());
        injector.getInstance(Framework.class).run();
    }

    @Override
    public void run() {
        LOGGER.info("Starting up EDDIE framework");
        jdbcAdapter.init();
        var connectionStatusMessageStream = JdkFlowAdapter.publisherToFlowPublisher(consentService.getConnectionStatusMessageStream());
        jdbcAdapter.setConnectionStatusMessageStream(connectionStatusMessageStream);
        var consumptionRecordMessageStream = JdkFlowAdapter.publisherToFlowPublisher(consumptionRecordService.getConsumptionRecordStream());
        jdbcAdapter.setConsumptionRecordStream(consumptionRecordMessageStream);
        javalinApp.init();
    }

    public static final class InitializationException extends RuntimeException {
        public InitializationException(String message) {
            super(message);
        }
    }
}
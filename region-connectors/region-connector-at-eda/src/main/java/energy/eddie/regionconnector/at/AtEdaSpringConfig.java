package energy.eddie.regionconnector.at;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.agnostic.SpringRegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.validated_historical_data.v0_82.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.processing.v0_82.ConsumptionRecordProcessor;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.EddieValidatedHistoricalDataMarketDocumentPublisher;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.at.eda.config.AtConfiguration.ELIGIBLE_PARTY_ID_KEY;
import static energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration.*;

@SpringBootConfiguration
@ComponentScan
@EnableWebMvc
@SpringRegionConnector(name = "at-eda")
public class AtEdaSpringConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtEdaSpringConfig.class);
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(AtEdaSpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            // Random port for this spring application, subject to change in GH-109
            ctx = app.run("--spring.config.import=", "--import.config.file=", "--server.port=0");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
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
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
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
                new SavingExtension<>(repository),
                new MessagingExtension<>(messages)
        );
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PermissionRequestCreationService creationService(PermissionRequestFactory permissionRequestFactory,
                                                            AtConfiguration configuration) {
        return new PermissionRequestCreationService(permissionRequestFactory, configuration);
    }

    @Bean
    public Supplier<Integer> portSupplier(ServletWebServerApplicationContext server) {
        return server.getWebServer()::getPort;
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RegionConnector regionConnector(
            PermissionRequestService permissionRequestService,
            ConsumptionRecordProcessor consumptionRecordProcessor,
            EdaAdapter edaAdapter,
            Sinks.Many<ConnectionStatusMessage> sink,
            Supplier<Integer> portSupplier
    ) throws TransmissionException {
        return new EdaRegionConnector(edaAdapter, permissionRequestService, consumptionRecordProcessor, sink, portSupplier);
    }

    @Bean
    public CommonInformationModelConfiguration commonInformationModelConfiguration(@Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingSchemeTypeList) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingSchemeTypeList));
    }

    @Bean
    public ConsumptionRecordProcessor consumptionRecordProcessor(
            PermissionRequestService permissionRequestService,
            CommonInformationModelConfiguration commonInformationModelConfiguration,
            EdaAdapter edaAdapter) {
        return new ConsumptionRecordProcessor(
                new ValidatedHistoricalDataMarketDocumentDirector(
                        commonInformationModelConfiguration,
                        new ValidatedHistoricalDataMarketDocumentBuilderFactory()
                ),
                new EddieValidatedHistoricalDataMarketDocumentPublisher(permissionRequestService),
                edaAdapter
        );
    }
}
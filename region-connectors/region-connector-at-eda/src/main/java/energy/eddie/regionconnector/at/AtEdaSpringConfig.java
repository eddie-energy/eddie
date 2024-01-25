package energy.eddie.regionconnector.at;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.at.eda.provider.EdaEddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.MessagingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.ConsentMarketDocumentExtension;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.Set;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.at.eda.config.AtConfiguration.CONVERSATION_ID_PREFIX;
import static energy.eddie.regionconnector.at.eda.config.AtConfiguration.ELIGIBLE_PARTY_ID_KEY;
import static energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration.*;

@SpringBootConfiguration
@ComponentScan
@EnableWebMvc
@RegionConnector(name = REGION_CONNECTOR_ID)
public class AtEdaSpringConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtEdaSpringConfig.class);

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
    public AtConfiguration atConfiguration(
            @Value("${" + ELIGIBLE_PARTY_ID_KEY + "}") String eligiblePartyId,
            @Value("${" + CONVERSATION_ID_PREFIX + ":#{null}}") @Nullable String conversationIdPrefix
    ) {
        return new PlainAtConfiguration(eligiblePartyId, conversationIdPrefix);
    }

    @Bean
    public AtPermissionRequestRepository permissionRequestRepository() {
        return new InMemoryPermissionRequestRepository();
    }

    @Bean
    public Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordStream(IdentifiableConsumptionRecordService identifiableConsumptionRecordService) {
        return identifiableConsumptionRecordService.getIdentifiableConsumptionRecordStream();
    }

    @Bean
    public Flux<ConsumptionRecord> consumptionRecordStream(EdaAdapter edaAdapter) {
        return edaAdapter.getConsumptionRecordStream();
    }

    @Bean
    public EdaAdapter edaAdapter(
            PontonXPAdapterConfiguration configuration,
            Environment environment
    ) throws JAXBException, IOException, ConnectionException {
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
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks
                .many()
                .multicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink() {
        return Sinks
                .many()
                .multicast()
                .onBackpressureBuffer();
    }

    @Bean
    public Set<Extension<AtPermissionRequest>> permissionRequestExtensions(AtPermissionRequestRepository repository,
                                                                           Sinks.Many<ConnectionStatusMessage> messages,
                                                                           Sinks.Many<ConsentMarketDocument> cmdSink,
                                                                           AtConfiguration configuration,
                                                                           CommonInformationModelConfiguration cimConfig) {
        return Set.of(
                new SavingExtension<>(repository),
                new MessagingExtension<>(messages),
                new ConsentMarketDocumentExtension<>(
                        cmdSink,
                        configuration.eligiblePartyId(),
                        cimConfig.eligiblePartyNationalCodingScheme().value()
                )
        );
    }

    @Bean
    public CommonInformationModelConfiguration commonInformationModelConfiguration(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingSchemeTypeList) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingSchemeTypeList));
    }

    @Bean
    public EdaEddieValidatedHistoricalDataMarketDocumentProvider consumptionRecordProcessor(
            CommonInformationModelConfiguration commonInformationModelConfiguration,
            Flux<IdentifiableConsumptionRecord> identfiableConsumptionRecordFlux) {
        return new EdaEddieValidatedHistoricalDataMarketDocumentProvider(
                new ValidatedHistoricalDataMarketDocumentDirector(
                        commonInformationModelConfiguration,
                        new ValidatedHistoricalDataMarketDocumentBuilderFactory()
                ),
                identfiableConsumptionRecordFlux
        );
    }

    @Bean
    public ConsentMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new CommonConsentMarketDocumentProvider(sink);
    }
}
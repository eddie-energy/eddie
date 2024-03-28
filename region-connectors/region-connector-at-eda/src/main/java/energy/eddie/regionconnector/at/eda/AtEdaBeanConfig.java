package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.ConsentMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.at.eda.provider.v0_82.EdaEddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.cmd.CommonConsentMarketDocumentProvider;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.at.eda.config.AtConfiguration.CONVERSATION_ID_PREFIX;
import static energy.eddie.regionconnector.at.eda.config.AtConfiguration.ELIGIBLE_PARTY_ID_KEY;
import static energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration.*;

@Configuration
public class AtEdaBeanConfig {
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PontonXPAdapterConfiguration pontonXPAdapterConfiguration(
            @Value("${" + ADAPTER_ID_KEY + "}") String adapterId,
            @Value("${" + ADAPTER_VERSION_KEY + "}") String adapterVersion,
            @Value("${" + HOSTNAME_KEY + "}") String hostname,
            @Value("${" + PORT_KEY + "}") int port,
            @Value("${" + WORK_FOLDER_KEY + "}") String workFolder
    ) {
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
    public Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordStream(
            IdentifiableConsumptionRecordService identifiableConsumptionRecordService
    ) {
        return identifiableConsumptionRecordService.getIdentifiableConsumptionRecordStream();
    }

    @Bean
    public Flux<ConsumptionRecord> consumptionRecordStream(EdaAdapter edaAdapter) {
        return edaAdapter.getConsumptionRecordStream();
    }

    @Bean
    @Profile("!no-ponton")
    public EdaAdapter edaAdapter(
            PontonXPAdapterConfiguration configuration,
            Jaxb2Marshaller jaxb2Marshaller,
            OutboundMessageFactoryCollection outboundMessageFactoryCollection
    ) throws IOException, ConnectionException {
        return new PontonXPAdapter(configuration, jaxb2Marshaller, outboundMessageFactoryCollection);
    }

    @Bean
    @Profile("no-ponton")
    public EdaAdapter noOpEdaAdapter() {
        return new NoOpEdaAdapter();
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
    public CommonInformationModelConfiguration commonInformationModelConfiguration(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingSchemeTypeList
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingSchemeTypeList));
    }

    @Bean
    public EdaEddieValidatedHistoricalDataMarketDocumentProvider consumptionRecordProcessor(
            CommonInformationModelConfiguration commonInformationModelConfiguration,
            Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux
    ) {
        return new EdaEddieValidatedHistoricalDataMarketDocumentProvider(
                new ValidatedHistoricalDataMarketDocumentDirector(
                        commonInformationModelConfiguration,
                        new ValidatedHistoricalDataMarketDocumentBuilderFactory()
                ),
                identifiableConsumptionRecordFlux
        );
    }

    @Bean
    public ConsentMarketDocumentProvider consentMarketDocumentProvider(Sinks.Many<ConsentMarketDocument> sink) {
        return new CommonConsentMarketDocumentProvider(sink);
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, PermissionEventRepository repository) {
        return new Outbox(eventBus, repository);
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("at.ebutilities.schemata.customerconsent");
        return marshaller;
    }

    @Bean
    public FulfillmentService fulfillmentService() {
        return new FulfillmentService();
    }
}

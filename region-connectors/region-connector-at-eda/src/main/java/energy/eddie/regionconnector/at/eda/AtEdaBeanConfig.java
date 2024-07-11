package energy.eddie.regionconnector.at.eda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies.EdaStrategy;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messenger.MessengerMonitor;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonMessengerConnection;
import energy.eddie.regionconnector.at.eda.ponton.messenger.WebClientMessengerHealth;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.at.eda.provider.v0_82.EdaEddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.at.eda.services.IdentifiableConsumptionRecordService;
import energy.eddie.regionconnector.at.eda.services.IdentifiableMasterDataService;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionEndIsEnergyDataEndStrategy;
import energy.eddie.spring.regionconnector.extensions.cim.v0_82.pmd.CommonPermissionMarketDocumentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.List;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.*;
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
            @Value("${" + API_ENDPOINT_KEY + "}") String apiEndpoint,
            @Value("${" + WORK_FOLDER_KEY + "}") String workFolder,
            @Value("${" + USERNAME_KEY + "}") String username,
            @Value("${" + PASSWORD_KEY + "}") String password
    ) {
        return new PlainPontonXPAdapterConfiguration(
                adapterId,
                adapterVersion,
                hostname,
                port,
                apiEndpoint,
                workFolder,
                username,
                password
        );
    }

    @Bean
    public AtConfiguration atConfiguration(
            @Value("${" + ELIGIBLE_PARTY_ID_KEY + "}") String eligiblePartyId
    ) {
        return new PlainAtConfiguration(eligiblePartyId);
    }

    @Bean
    public Flux<IdentifiableConsumptionRecord> consumptionRecordStream(EdaAdapter edaAdapter) {
        return edaAdapter.getConsumptionRecordStream();
    }

    @Bean
    public Flux<IdentifiableMasterData> masterDataStream(EdaAdapter edaAdapter) {
        return edaAdapter.getMasterDataStream();
    }

    @Bean
    @Profile("!no-ponton")
    public EdaAdapter edaAdapter(
            PontonMessengerConnection pontonMessengerConnection,
            IdentifiableConsumptionRecordService identifiableConsumptionRecordService,
            IdentifiableMasterDataService identifiableMasterDataService,
            TaskScheduler taskScheduler
    ) {
        return new PontonXPAdapter(pontonMessengerConnection,
                                   identifiableConsumptionRecordService,
                                   identifiableMasterDataService,
                                   taskScheduler);
    }

    @Bean
    public PontonMessengerConnection pontonMessengerConnection(
            PontonXPAdapterConfiguration configuration,
            InboundMessageFactoryCollection inboundMessageFactoryCollection,
            OutboundMessageFactoryCollection outboundMessageFactoryCollection,
            WebClient webClient,
            MessengerMonitor messengerMonitor
    ) throws ConnectionException, IOException {
        return PontonMessengerConnection
                .newBuilder()
                .withConfig(configuration)
                .withInboundMessageFactoryCollection(inboundMessageFactoryCollection)
                .withOutboundMessageFactoryCollection(outboundMessageFactoryCollection)
                .withHealthApi(new WebClientMessengerHealth(webClient, configuration))
                .withMessengerMonitor(messengerMonitor)
                .build();
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
    public Sinks.Many<PermissionEnveloppe> consentMarketDocumentSink() {
        return Sinks
                .many()
                .multicast()
                .onBackpressureBuffer();
    }

    @Bean
    public CommonInformationModelConfiguration cimConfig(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
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
    public PermissionMarketDocumentProvider permissionMarketDocumentProvider(Sinks.Many<PermissionEnveloppe> sink) {
        return new CommonPermissionMarketDocumentProvider(sink);
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
        marshaller.setPackagesToScan("at.ebutilities.schemata");
        return marshaller;
    }

    @Bean
    public FulfillmentService fulfillmentService(Outbox outbox) {
        return new FulfillmentService(outbox, SimpleEvent::new);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    public TransmissionScheduleProvider<PermissionRequest> transmissionScheduleProvider() {
        return pr -> TRANSMISSION_CYCLE.name();
    }

    @Bean
    public ConnectionStatusMessageHandler<AtPermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            Sinks.Many<ConnectionStatusMessage> messages,
            AtPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                messages,
                repository,
                AtPermissionRequest::message,
                pr -> objectMapper().createObjectNode().put("cmRequestId", pr.cmRequestId())
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<AtPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            AtPermissionRequestRepository repository,
            Sinks.Many<PermissionEnveloppe> pmdSink,
            AtConfiguration atConfig,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                            repository,
                                                            pmdSink,
                                                            atConfig.eligiblePartyId(),
                                                            cimConfig,
                                                         pr -> TRANSMISSION_CYCLE.name(),
                                                            AT_ZONE_ID);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService() {
        return new DataNeedCalculationServiceImpl(
                SUPPORTED_DATA_NEEDS,
                EdaRegionConnectorMetadata.getInstance(),
                new PermissionEndIsEnergyDataEndStrategy(AT_ZONE_ID),
                new EdaStrategy(),
                List.of()
        );
    }
}

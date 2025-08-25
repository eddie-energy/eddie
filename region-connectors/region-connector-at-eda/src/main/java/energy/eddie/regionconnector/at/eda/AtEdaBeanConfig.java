package energy.eddie.regionconnector.at.eda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies.EdaStrategy;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.persistence.EdaPermissionEventRepository;
import energy.eddie.regionconnector.at.eda.ponton.NoOpEdaAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PlainPontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapter;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messenger.MessengerMonitor;
import energy.eddie.regionconnector.at.eda.ponton.messenger.PontonMessengerConnection;
import energy.eddie.regionconnector.at.eda.ponton.messenger.WebClientMessengerHealth;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.TRANSMISSION_CYCLE;
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
            AtPermissionRequestRepository repository,
            ObjectMapper objectMapper
    ) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                repository,
                AtPermissionRequest::message,
                pr -> objectMapper.createObjectNode().put("cmRequestId", pr.cmRequestId())
        );
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapper() {
        return customizer -> customizer
                .modules(new JavaTimeModule(), new Jdk8Module());
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<AtPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            AtPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            AtConfiguration atConfig,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                            repository,
                                                            dataNeedsService,
                                                            atConfig.eligiblePartyId(),
                                                            cimConfig,
                                                            pr -> TRANSMISSION_CYCLE.name(),
                                                            AT_ZONE_ID);
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                EdaRegionConnectorMetadata.getInstance(),
                new PermissionEndIsEnergyDataEndStrategy(),
                new EdaStrategy(),
                List.of()
        );
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(EdaPermissionEventRepository repo) {
        return () -> repo;
    }
}

package energy.eddie.regionconnector.at.eda;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies.EdaStrategy;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.persistence.EdaPermissionEventRepository;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.Supplier;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.TRANSMISSION_CYCLE;

@Configuration
@EnableConfigurationProperties({AtConfiguration.class, PontonXPAdapterConfiguration.class})
public class AtEdaBeanConfig {
    @Bean
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
    @Primary
    @ConditionalOnProperty(value = "region-connector.at.eda.ponton.messenger.enabled", havingValue = "true", matchIfMissing = true)
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
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            DataNeedRuleSet ruleSet
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                EdaRegionConnectorMetadata.getInstance(),
                new PermissionEndIsEnergyDataEndStrategy(),
                new EdaStrategy(),
                ruleSet
        );
    }

    @Bean
    Supplier<PermissionEventRepository> permissionEventSupplier(EdaPermissionEventRepository repo) {
        return () -> repo;
    }
}

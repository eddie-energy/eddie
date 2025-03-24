package energy.eddie.regionconnector.cds.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneOffset;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Configuration
public class ProviderConfig {
    @Bean
    public ConnectionStatusMessageProvider connectionStatusMessageProvider(
            EventBus eventBus,
            CdsPermissionRequestRepository repository,
            ObjectMapper objectMapper
    ) {
        var redirectUriJsonNode = new RedirectUriJsonNode(objectMapper);
        return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "", redirectUriJsonNode);
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Bean
    @OnRawDataMessagesEnabled
    public JsonRawDataProvider jsonRawDataProvider(ObjectMapper objectMapper, IdentifiableDataStreams streams) {
        return new JsonRawDataProvider(REGION_CONNECTOR_ID, objectMapper, streams.usageSegments());
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public PermissionMarketDocumentMessageHandler<CdsPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            CdsPermissionRequestRepository cdsPermissionRequestRepository,
            DataNeedsService dataNeedsService,
            CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                            cdsPermissionRequestRepository,
                                                            dataNeedsService,
                                                            cimConfig.eligiblePartyFallbackId(),
                                                            cimConfig,
                                                            pr -> null,
                                                            ZoneOffset.UTC);
    }
}

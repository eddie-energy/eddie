package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.ZoneOffset;

import static energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata.REGION_CONNECTOR_ID;

/**
 * Main Spring Boot Application configuration for the German (DE) ETA Plus
 * region connector.
 * This class serves as the entry point for the region connector module.
 */
@EnableWebMvc
@EnableScheduling
@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
public class DeEtaSpringConfig {

    // For connection status messages
    @Bean
    public ConnectionStatusMessageHandler<DePermissionRequest> connectionStatusMessageHandler(
            EventBus eventBus,
            DePermissionRequestRepository repository) {
        return new ConnectionStatusMessageHandler<>(
                eventBus,
                repository,
                pr -> "");
    }

    // For permission market documents, the CIM pendant to connection status
    // messages
    @Bean
    public PermissionMarketDocumentMessageHandler<DePermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            DePermissionRequestRepository repo,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                repo,
                dataNeedsService,
                cimConfig.eligiblePartyFallbackId(),
                cimConfig,
                pr -> null,
                ZoneOffset.UTC);
    }
}

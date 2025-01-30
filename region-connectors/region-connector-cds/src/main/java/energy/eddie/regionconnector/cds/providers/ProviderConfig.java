package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {
    @Bean
    public ConnectionStatusMessageProvider connectionStatusMessageProvider(
            EventBus eventBus,
            CdsPermissionRequestRepository repository
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, repository, pr -> "");
    }
}

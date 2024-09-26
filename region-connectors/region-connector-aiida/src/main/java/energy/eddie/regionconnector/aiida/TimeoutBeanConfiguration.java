package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeoutBeanConfiguration {

    @Bean
    public CommonTimeoutService timeoutService(
            AiidaPermissionRequestViewRepository repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration
    ) {
        return new CommonTimeoutService(
                repository,
                SimpleEvent::new,
                outbox,
                timeoutConfiguration,
                AiidaRegionConnectorMetadata.getInstance()
        );
    }
}

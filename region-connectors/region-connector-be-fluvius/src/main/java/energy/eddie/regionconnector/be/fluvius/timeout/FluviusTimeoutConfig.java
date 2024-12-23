package energy.eddie.regionconnector.be.fluvius.timeout;

import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FluviusTimeoutConfig {
    @Bean
    public CommonTimeoutService timeoutService(
            BePermissionRequestRepository bePermissionRequestRepository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration,
            RegionConnectorMetadata metadata
    ) {
        return new CommonTimeoutService(
                bePermissionRequestRepository,
                SimpleEvent::new,
                outbox,
                timeoutConfiguration,
                metadata
        );
    }
}

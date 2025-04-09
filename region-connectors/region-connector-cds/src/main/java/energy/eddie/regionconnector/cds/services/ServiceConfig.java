package energy.eddie.regionconnector.cds.services;

import energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ServiceConfig {
    @Bean
    public CommonTimeoutService commonTimeoutService(
            CdsPermissionRequestRepository cdsPermissionRequestRepository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration,
            CdsRegionConnectorMetadata cdsRegionConnectorMetadata
    ) {
        return new CommonTimeoutService(
                cdsPermissionRequestRepository,
                SimpleEvent::new,
                outbox,
                timeoutConfiguration,
                cdsRegionConnectorMetadata
        );
    }
}

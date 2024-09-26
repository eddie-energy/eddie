package energy.eddie.regionconnector.fr.enedis;


import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeoutBeanConfiguration {

    @Bean
    public CommonTimeoutService timeoutService(
            FrPermissionRequestRepository repository,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            TimeoutConfiguration timeoutConfiguration
    ) {
        return new CommonTimeoutService(
                repository,
                FrSimpleEvent::new,
                outbox,
                timeoutConfiguration,
                EnedisRegionConnectorMetadata.getInstance()
        );
    }
}

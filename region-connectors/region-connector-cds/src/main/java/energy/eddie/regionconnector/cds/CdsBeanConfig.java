package energy.eddie.regionconnector.cds;

import energy.eddie.regionconnector.cds.config.CdsConfiguration;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = CdsConfiguration.class)
public class CdsBeanConfig {
    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }
}

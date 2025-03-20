package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.services.PollingService;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@EntityScan
@Configuration
public class EnedisPersistenceConfig {
    @Bean
    public CommonFutureDataService<FrEnedisPermissionRequest> commonFutureDataService(
            PollingService pollingService,
            FrPermissionRequestRepository repository,
            EnedisRegionConnector connector
    ){
        return new CommonFutureDataService<>(
                pollingService,
                repository,
                "0 0 17 * * *",
                connector.getMetadata()
        );
    }
}

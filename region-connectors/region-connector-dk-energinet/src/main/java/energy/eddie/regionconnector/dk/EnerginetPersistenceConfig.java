package energy.eddie.regionconnector.dk;

import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.services.PollingService;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@EntityScan
@Configuration
public class EnerginetPersistenceConfig {
    @Bean
    public CommonFutureDataService<DkEnerginetPermissionRequest> commonFutureDataService(
            @Qualifier("pollingService") PollingService pollingService,
            DkPermissionRequestRepository repository,
            EnerginetRegionConnector connector
    ){
        return new CommonFutureDataService<>(
                pollingService,
                repository,
                "0 0 17 * * *",
                connector.getMetadata()
        );
    }
}

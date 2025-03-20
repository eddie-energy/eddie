package energy.eddie.regionconnector.es.datadis;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.DataApiService;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@EntityScan
@Configuration
public class DatadisPersistenceConfig {
    @Bean
    public CommonFutureDataService<EsPermissionRequest> commonFutureDataService(
            DataApiService apiService,
            EsPermissionRequestRepository repository,
            DatadisRegionConnector connector
    ){
        return new CommonFutureDataService<>(
                apiService,
                repository,
                "0 0 17 * * *",
                connector.getMetadata()
        );
    }
}

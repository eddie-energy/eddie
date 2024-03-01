package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackageClasses = JpaPermissionRequestRepository.class)
@EntityScan(basePackageClasses = {AtEdaPersistenceConfig.class, EdaPermissionRequest.class})
@Configuration
public class AtEdaPersistenceConfig {
}

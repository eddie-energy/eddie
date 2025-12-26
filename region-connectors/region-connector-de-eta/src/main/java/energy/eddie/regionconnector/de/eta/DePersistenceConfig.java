package energy.eddie.regionconnector.de.eta;

import energy.eddie.regionconnector.de.eta.permission.request.events.PersistablePermissionEvent;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionEventRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Persistence configuration for the German (DE) ETA Plus region connector.
 * This configuration enables Spring Data JPA repository scanning and entity scanning.
 * 
 * Uses explicit basePackageClasses to ensure Spring finds repositories and entities
 * in the de.eta.persistence and de.eta.permission.request.events packages.
 */
@EnableJpaRepositories(basePackageClasses = DePermissionEventRepository.class)
@EntityScan(basePackageClasses = {DePersistenceConfig.class, PersistablePermissionEvent.class})
@Configuration
public class DePersistenceConfig {
}

package energy.eddie.regionconnector.de.eta.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Persistence configuration for the German (DE) ETA Plus region connector.
 * This configuration enables Spring Data JPA repository scanning and entity scanning.
 */
@EnableJpaRepositories
@Configuration
public class DePersistenceConfig {
}


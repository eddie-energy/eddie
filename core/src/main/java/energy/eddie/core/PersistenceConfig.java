package energy.eddie.core;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

// The empty persistence config is needed, so it is not loaded during testing.
// Even when excluding configs, spring boot tests load the main application config
// EntityScan with base package is required so that the JPA repositories in child context will be initialized correctly
@EntityScan(basePackages = "energy.eddie")
@Configuration
public class PersistenceConfig {
}

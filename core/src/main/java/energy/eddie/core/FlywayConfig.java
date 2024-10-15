package energy.eddie.core;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static energy.eddie.spring.OutboundConnectorRegistrationBeanPostProcessor.ENABLED_OUTBOUND_CONNECTOR_BEAN_NAME;
import static energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor.ENABLED_REGION_CONNECTOR_BEAN_NAME;

@Configuration
public class FlywayConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Creates a {@link FlywayMigrationStrategy} for each enabled region-connector and outbound-connector, as well as for the {@code core},
     * {@code data-needs} and {@code admin-console} module. The migration strategy creates the schema for the module and
     * executes any migration scripts found in the respective folders on the classpath. The folder pattern is:
     * "db/migration/&lt;region-connector-name&gt;" or "db/migration/&lt;outbound-connector-name&gt;".
     * Any minus ('-') in the region-connector's and outbound-connector's name will be replaced by an underscore ('_') for a valid schema name.
     *
     * @param enabledOutboundConnectorNames List of all the region connector names.
     * @param enabledRegionConnectorNames   List of all the region connector names.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            @Qualifier(ENABLED_OUTBOUND_CONNECTOR_BEAN_NAME) List<String> enabledOutboundConnectorNames,
            @Qualifier(ENABLED_REGION_CONNECTOR_BEAN_NAME) List<String> enabledRegionConnectorNames
    ) {
        List<String> modulesForFlyway = new ArrayList<>(enabledOutboundConnectorNames);
        modulesForFlyway.addAll(enabledRegionConnectorNames);
        modulesForFlyway.add("core");
        modulesForFlyway.add("data-needs");
        return flyway ->
                // also execute flyway migration for core
                modulesForFlyway.forEach(target -> {
                    LOGGER.info("Starting Flyway migration for '{}'", target);
                    var schemaName = target.replace('-', '_');
                    Flyway.configure()
                          .configuration(flyway.getConfiguration())
                          .schemas(schemaName)
                          .locations("db/migration/" + target)
                          .load()
                          .migrate();
                });
    }
}

package energy.eddie.regionconnector.dk.energinet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * @param customerBasePath BasePath for the customer api
 */
@ConfigurationProperties("region-connector.dk.energinet")
public record EnerginetConfiguration(
        @Name("customer.client.basepath") String customerBasePath
) {}

package energy.eddie.regionconnector.fr.enedis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * @param clientId     Client ID that will be used to authenticate with Enedis. Must be from an Application registered with Enedis.
 * @param clientSecret Client Secret that will be used to authenticate with Enedis. Must be from an Application registered with Enedis.
 * @param basePath     BasePath is optional and can be changed to the sandbox environment of Enedis for testing - default is production.
 */
@ConfigurationProperties("region-connector.fr.enedis")
public record EnedisConfiguration(
        @Name("client.id") String clientId,
        @Name("client.secret") String clientSecret,
        @Name("basepath") String basePath
) {}

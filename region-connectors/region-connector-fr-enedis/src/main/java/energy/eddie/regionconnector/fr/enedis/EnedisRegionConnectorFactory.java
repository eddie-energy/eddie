package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientDecorator;
import energy.eddie.regionconnector.fr.enedis.client.HealthCheckedEnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.ConfigEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.eclipse.microprofile.config.Config;

public class EnedisRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        EnedisConfiguration configuration = new ConfigEnedisConfiguration(config);
        EnedisApi enedisApi = new HealthCheckedEnedisApi(
                new EnedisApiClientDecorator(
                        new EnedisApiClient(configuration)
                )
        );

        return new EnedisRegionConnector(configuration, enedisApi);
    }

}

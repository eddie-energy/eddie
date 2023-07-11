package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorProvisioningException;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientDecorator;
import energy.eddie.regionconnector.fr.enedis.config.ConfigEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.eclipse.microprofile.config.Config;

public class EnedisRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) throws RegionConnectorProvisioningException {
        EnedisConfiguration configuration = new ConfigEnedisConfiguration(config);

        var enedisApi = new EnedisApiClientDecorator(configuration);

        return new EnedisRegionConnector(configuration, enedisApi);
    }

}

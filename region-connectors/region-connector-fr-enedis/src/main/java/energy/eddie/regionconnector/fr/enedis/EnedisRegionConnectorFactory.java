package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorProvisioningException;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientDecorator;
import energy.eddie.regionconnector.fr.enedis.config.ConfigEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class EnedisRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) throws RegionConnectorProvisioningException {
        requireNonNull(config);

        EnedisConfiguration configuration;
        try {
            configuration = new ConfigEnedisConfiguration(config);
        } catch (Exception e) {
            throw new RegionConnectorProvisioningException(e);
        }

        EnedisApi enedisApi = new EnedisApiClientDecorator(configuration);

        return new EnedisRegionConnector(configuration, enedisApi);
    }

}

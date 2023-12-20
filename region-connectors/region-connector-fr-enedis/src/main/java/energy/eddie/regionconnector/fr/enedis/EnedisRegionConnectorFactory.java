package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import org.eclipse.microprofile.config.Config;

public class EnedisRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        return SpringConfig.start();
    }
}
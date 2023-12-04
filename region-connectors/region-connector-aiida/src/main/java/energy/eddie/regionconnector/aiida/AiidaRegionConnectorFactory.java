package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import org.eclipse.microprofile.config.Config;

public class AiidaRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        return AiidaSpringConfig.start();
    }
}

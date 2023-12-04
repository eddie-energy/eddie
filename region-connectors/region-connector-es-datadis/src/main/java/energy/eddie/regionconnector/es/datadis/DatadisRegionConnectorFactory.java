package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import org.eclipse.microprofile.config.Config;

public class DatadisRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        return DatadisSpringConfig.start();
    }
}

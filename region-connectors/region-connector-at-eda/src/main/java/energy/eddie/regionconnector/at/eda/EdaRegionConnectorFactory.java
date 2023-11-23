package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorProvisioningException;
import energy.eddie.regionconnector.at.SpringConfig;
import org.eclipse.microprofile.config.Config;

public class EdaRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    // sonarcloud complains about not using try-with-resources, but using it would close the adapter upon returning, which is not what we want
    @SuppressWarnings("java:S2095")
    public RegionConnector create(Config config) throws RegionConnectorProvisioningException {
        return SpringConfig.start();
    }
}

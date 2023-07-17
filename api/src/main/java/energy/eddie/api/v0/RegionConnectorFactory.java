package energy.eddie.api.v0;

import org.eclipse.microprofile.config.Config;

public interface RegionConnectorFactory {

    /**
     * Creates a new RegionConnector based on the given configuration.
     *
     * @param config the configuration to use
     * @return a new RegionConnector
     * @throws RegionConnectorProvisioningException if the RegionConnector could not be created
     */
    RegionConnector create(Config config) throws RegionConnectorProvisioningException;
}

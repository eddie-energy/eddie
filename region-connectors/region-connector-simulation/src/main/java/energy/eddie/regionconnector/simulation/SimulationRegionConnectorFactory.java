package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.RegionConnectorProvisioningException;
import org.eclipse.microprofile.config.Config;

public class SimulationRegionConnectorFactory implements RegionConnectorFactory {

    @Override
    public RegionConnector create(Config config) throws RegionConnectorProvisioningException {
        return new SimulationConnector();
    }
}

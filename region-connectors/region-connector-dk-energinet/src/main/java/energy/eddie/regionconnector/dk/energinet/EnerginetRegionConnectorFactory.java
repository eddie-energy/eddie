package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.dk.SpringConfig;
import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class EnerginetRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        requireNonNull(config);
        return SpringConfig.start(config);
    }

}

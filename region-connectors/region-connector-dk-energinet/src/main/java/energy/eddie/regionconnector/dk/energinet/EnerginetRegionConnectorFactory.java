package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.dk.energinet.config.ConfigEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import org.eclipse.microprofile.config.Config;

public class EnerginetRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        EnerginetConfiguration configuration = new ConfigEnerginetConfiguration(config);
        EnerginetCustomerApiClient energinetCustomerApi = new EnerginetCustomerApiClient(configuration);

        return new EnerginetRegionConnector(configuration, energinetCustomerApi);
    }

}

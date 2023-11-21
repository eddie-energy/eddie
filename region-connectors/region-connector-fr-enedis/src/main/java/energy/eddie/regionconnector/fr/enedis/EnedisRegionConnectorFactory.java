package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClientDecorator;
import energy.eddie.regionconnector.fr.enedis.client.HealthCheckedEnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.ConfigEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.InMemoryPermissionRequestRepository;
import org.eclipse.microprofile.config.Config;

public class EnedisRegionConnectorFactory implements RegionConnectorFactory {
    @Override
    public RegionConnector create(Config config) {
        EnedisConfiguration configuration = new ConfigEnedisConfiguration(config);
        PermissionRequestRepository<TimeframedPermissionRequest> repository = new InMemoryPermissionRequestRepository();
        EnedisApi enedisApi = new HealthCheckedEnedisApi(
                new EnedisApiClientDecorator(
                        new EnedisApiClient(configuration)
                )
        );

        return new EnedisRegionConnector(configuration, enedisApi, repository);
    }
}
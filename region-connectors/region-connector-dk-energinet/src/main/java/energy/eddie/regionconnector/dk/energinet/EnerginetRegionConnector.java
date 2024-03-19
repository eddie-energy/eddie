package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static java.util.Objects.requireNonNull;

@Component
public class EnerginetRegionConnector implements RegionConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetRegionConnector.class);
    private final EnerginetCustomerApi energinetCustomerApi;
    private final PermissionRequestService permissionRequestService;

    public EnerginetRegionConnector(
            EnerginetCustomerApi energinetCustomerApi,
            PermissionRequestService permissionRequestService
    ) {
        this.energinetCustomerApi = requireNonNull(energinetCustomerApi);
        this.permissionRequestService = requireNonNull(permissionRequestService);
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EnerginetRegionConnectorMetadata.getInstance();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var permissionRequest = permissionRequestService.findByPermissionId(permissionId);
        if (permissionRequest.isEmpty()) {
            return;
        }
        try {
            permissionRequest.get().terminate();
        } catch (StateTransitionException e) {
            LOGGER.error("PermissionRequest with permissionID {} cannot be revoked", permissionId, e);
        }
    }

    @Override
    public Map<String, HealthState> health() {
        return energinetCustomerApi.health().block();
    }
}

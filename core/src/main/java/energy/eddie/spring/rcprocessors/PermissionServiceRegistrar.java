package energy.eddie.spring.rcprocessors;


import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.core.services.PermissionService;
import energy.eddie.spring.RegionConnectorProcessor;

import static java.util.Objects.requireNonNull;

@RegionConnectorProcessor
public class PermissionServiceRegistrar {
    public PermissionServiceRegistrar(Mvp1ConnectionStatusMessageProvider statusMessageProvider, PermissionService permissionService) {
        requireNonNull(statusMessageProvider);
        requireNonNull(permissionService);
        permissionService.registerProvider(statusMessageProvider);
    }
}

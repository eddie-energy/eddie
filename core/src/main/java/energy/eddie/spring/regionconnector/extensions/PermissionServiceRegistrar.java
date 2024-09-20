package energy.eddie.spring.regionconnector.extensions;


import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.core.services.MetadataService;
import energy.eddie.core.services.PermissionService;

import static java.util.Objects.requireNonNull;

/**
 * The {@code PermissionServiceRegistrar} should be added to each region connector's own context and will register the
 * {@link ConnectionStatusMessageProvider} of each region connector to the common {@link MetadataService}. Each region
 * connector implementation is required to provide an implementation of the {@code ConnectionStatusMessageProvider}
 * interface.
 */
@RegionConnectorExtension
public class PermissionServiceRegistrar {
    public PermissionServiceRegistrar(
            ConnectionStatusMessageProvider statusMessageProvider,
            PermissionService permissionService
    ) {
        requireNonNull(statusMessageProvider);
        requireNonNull(permissionService);
        permissionService.registerProvider(statusMessageProvider);
    }
}

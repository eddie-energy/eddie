package energy.eddie.spring.regionconnector.extensions;


import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageProvider;
import energy.eddie.core.services.MetadataService;
import energy.eddie.core.services.PermissionService;

import static java.util.Objects.requireNonNull;

/**
 * The {@code PermissionServiceRegistrar} should be added to each region connector's own context and will
 * register the {@link Mvp1ConnectionStatusMessageProvider} of each region connector to the common {@link MetadataService}.
 * Each region connector implementation is required to provide an implementation of the {@code Mvp1ConnectionStatusMessageProvider} interface.
 */
@RegionConnectorExtension
public class PermissionServiceRegistrar {
    public PermissionServiceRegistrar(Mvp1ConnectionStatusMessageProvider statusMessageProvider, PermissionService permissionService) {
        requireNonNull(statusMessageProvider);
        requireNonNull(permissionService);
        permissionService.registerProvider(statusMessageProvider);
    }
}

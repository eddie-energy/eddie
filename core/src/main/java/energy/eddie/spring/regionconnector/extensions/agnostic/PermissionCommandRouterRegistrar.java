// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.agnostic;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.command.RegionConnectorPermissionCommandService;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.agnostic.PermissionCommandRouter;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The {@code PermissionCommandRouterRegistrar} should be added to each region connector's own context and will
 * register the {@link RegionConnector} of each region connector to the common {@link PermissionCommandRouterRegistrar}.
 * Each region connector implementation is required to provide an implementation of the {@code RegionConnector} interface.
 */
@RegionConnectorExtension
public class PermissionCommandRouterRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    // In this case the permission command router might be nullable.
    public PermissionCommandRouterRegistrar(
            RegionConnector regionConnector,
            Optional<RegionConnectorPermissionCommandService> permissionCommandService,
            Optional<PermissionCommandRouter> permissionCommandRouter
    ) {
        requireNonNull(regionConnector);
        requireNonNull(permissionCommandService);
        requireNonNull(permissionCommandRouter);

        if (permissionCommandService.isEmpty() || permissionCommandRouter.isEmpty()) {
            return;
        }

        permissionCommandRouter.get().registerPermissionCommandService(
                regionConnector.getMetadata().id(),
                permissionCommandService.get()
        );
    }
}

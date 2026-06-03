// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.agnostic;

import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.api.agnostic.outbound.PermissionCommandOutboundConnector;
import energy.eddie.core.services.agnostic.PermissionCommandRouter;

import java.util.Optional;

@OutboundConnectorExtension
public class PermissionCommandOutboundConnectorRegistrar {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public PermissionCommandOutboundConnectorRegistrar(
            Optional<PermissionCommandOutboundConnector> permissionCommandOutboundConnector,
            PermissionCommandRouter router
    ) {
        permissionCommandOutboundConnector.ifPresent(router::registerPermissionCommandConnector);
    }
}

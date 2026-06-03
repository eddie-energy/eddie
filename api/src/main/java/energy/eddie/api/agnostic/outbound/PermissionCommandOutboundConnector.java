// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.outbound;

import energy.eddie.cim.agnostic.PermissionCommand;
import reactor.core.publisher.Flux;

/**
 * A permission command connector allows the eligible party to send permission commands to a certain region-connector.
 * This is used for example to update the transmission schedule of an AIIDA permission.
 */
public interface PermissionCommandOutboundConnector {
    /**
     * A flux of {@link PermissionCommand}s, to route the permission commands to the correct region-connector.
     *
     * @return Contains the regionConnectorId, the permissionID, the timestamp and additional properties per command action
     */
    Flux<PermissionCommand> getPermissionCommands();
}

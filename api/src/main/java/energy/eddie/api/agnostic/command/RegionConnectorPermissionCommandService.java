// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.command;

import energy.eddie.cim.agnostic.PermissionCommand;

public interface RegionConnectorPermissionCommandService {
    /**
     * This method is called when an permission command arrives at the region connector.
     * The implementation should handle the message accordingly.
     *
     * @param permissionCommand The permission command that has arrived.
     */
    void permissionCommandArrived(PermissionCommand permissionCommand);
}

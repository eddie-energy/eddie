// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class PermissionNotMonitorableException extends Exception {
    public PermissionNotMonitorableException(UUID permissionId) {
        super("Permission with ID '%s' is not an active inbound permission receiving connection limits."
                      .formatted(permissionId));
    }
}

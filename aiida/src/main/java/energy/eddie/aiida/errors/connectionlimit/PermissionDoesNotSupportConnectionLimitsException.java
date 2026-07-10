// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.connectionlimit;

import java.util.UUID;

public class PermissionDoesNotSupportConnectionLimitsException extends Exception {
    public PermissionDoesNotSupportConnectionLimitsException(UUID permissionId) {
        super("Permission with ID '%s' does not support connection limits.".formatted(permissionId));
    }
}

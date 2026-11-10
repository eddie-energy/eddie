// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class InvalidOutboundPermissionException extends Exception {
    public InvalidOutboundPermissionException(UUID permissionId) {
        super("Permission with ID '%s' is not an outbound permission.".formatted(permissionId));
    }
}

// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.UUID;

/**
 * Thrown to indicate that no permission with the specified ID is saved in this AIIDA instance.
 */
public class PermissionNotFoundException extends Exception {
    /**
     * Constructs an PermissionNotFoundException with the default message, that includes the permissionId.
     *
     * @param permissionId ID of the permission that could not be found.
     */
    public PermissionNotFoundException(UUID permissionId) {
        super("No permission with ID '%s' found.".formatted(permissionId));
    }
}

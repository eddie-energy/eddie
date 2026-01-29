// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class PermissionAlreadyExistsException extends Exception {
    public PermissionAlreadyExistsException(UUID permissionId) {
        super("Permission with ID '%s' already exists.".formatted(permissionId));
    }
}

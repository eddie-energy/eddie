// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import jakarta.annotation.Nullable;

import java.util.UUID;

public class PermissionUnfulfillableException extends Exception {
    public PermissionUnfulfillableException(@Nullable UUID permissionId) {
        super("Permission %s cannot be fulfilled by your AIIDA.".formatted(permissionId));
    }
}

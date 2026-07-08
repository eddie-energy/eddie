// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import jakarta.annotation.Nullable;

import java.util.UUID;

public class PermissionDataNeedTypeNotSupportedException extends Exception {
    public PermissionDataNeedTypeNotSupportedException(@Nullable UUID permissionId, String type) {
        super("Permission %s cannot be fulfilled: the data need type %s is not supported by this AIIDA instance.".formatted(
                permissionId,
                type));
    }
}

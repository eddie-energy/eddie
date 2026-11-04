// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import java.util.Set;

/**
 * Static state of a permission. Permissions are only persisted once the user accepted them, so every permission starts
 * as {@link #ACCEPTED} and ends in one of the final states. The dynamic connection state of an accepted permission is
 * described by {@link ConnectionStatus}.
 */
public enum PermissionStatus {
    /**
     * The user accepted the permission request.
     */
    ACCEPTED,
    /**
     * The user revoked the permission.
     */
    REVOKED,
    /**
     * The permission was terminated by the eligible party.
     */
    TERMINATED,
    /**
     * The expiration time of the permission was reached.
     */
    FULFILLED;

    public static final Set<PermissionStatus> ACTIVE = Set.of(ACCEPTED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}

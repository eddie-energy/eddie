// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import java.util.Set;

public enum PermissionStatus {
    /**
     * The permission has been created on AIIDA.
     */
    CREATED,
    /**
     * The details, like start and end date, of the permission have been fetched from the EDDIE framework.
     */
    FETCHED_DETAILS,
    /**
     * This AIIDA instance is unable to fulfill the permission request, e.g. because the requested data is not
     * available.
     */
    UNFULFILLABLE,
    /**
     * The user accepted the permission request. The dynamic connection state of an accepted permission is described
     * by {@link ConnectionStatus}.
     */
    ACCEPTED,
    /**
     * The MQTT credentials and topics for the permission have been fetched from the EDDIE framework.
     */
    FETCHED_MQTT_CREDENTIALS,
    /**
     * The user rejected the permission request.
     */
    REJECTED,
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

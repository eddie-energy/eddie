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
     * The user accepted the permission request.
     */
    ACCEPTED,
    /**
     * The MQTT credentials and topics for the permission have been fetched from the EDDIE framework.
     */
    FETCHED_MQTT_CREDENTIALS,
    /**
     * The permission has been accepted but waits to connect to the given infrastructure, as the start of the
     * permission is scheduled at a later point in time.
     */
    WAITING_FOR_CONNECTION,
    /**
     * The permission has been accepted and connected to the given infrastructure on start-up.
     */
    CONNECTED,
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
    FULFILLED,
    /**
     * The permission has been accepted but failed to connect to the given infrastructure on start-up.
     */
    NOT_CONNECTED;

    public static final Set<PermissionStatus> CONNECTION_STATES = Set.of(WAITING_FOR_CONNECTION, CONNECTED);
    public static final Set<PermissionStatus> ACTIVE = Set.of(ACCEPTED, WAITING_FOR_CONNECTION, CONNECTED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}

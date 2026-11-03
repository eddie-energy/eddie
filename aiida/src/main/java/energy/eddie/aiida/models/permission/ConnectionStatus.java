// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

/**
 * Dynamic connection state of an {@link PermissionStatus#ACCEPTED} permission. In contrast to the static
 * {@link PermissionStatus}, the connection state has no start or end point and may change as often as necessary. It is
 * not persisted, but derived at runtime from the start time of the permission and the state of its streamer.
 */
public enum ConnectionStatus {
    /**
     * The permission waits to connect to the given infrastructure, as the start of the permission is scheduled at a
     * later point in time.
     */
    WAITING_FOR_CONNECTION,
    /**
     * The permission is not connected to the given infrastructure, e.g. because the connection failed on start-up or
     * was lost.
     */
    NOT_CONNECTED,
    /**
     * The permission is connected to the given infrastructure.
     */
    CONNECTED
}

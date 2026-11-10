// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.permission.PermissionDto;
import energy.eddie.aiida.models.permission.ConnectionStatus;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.streamers.StreamerManager;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Derives the dynamic {@link ConnectionStatus} of a permission at runtime. The connection status is intentionally not
 * persisted, as it changes frequently and must always reflect the actual state of the streamer.
 */
@Service
public class ConnectionStatusService {
    private final Clock clock;
    private final StreamerManager streamerManager;

    public ConnectionStatusService(Clock clock, StreamerManager streamerManager) {
        this.clock = clock;
        this.streamerManager = streamerManager;
    }

    /**
     * Returns the connection status of the passed permission, or {@code null} if the permission is not
     * {@link PermissionStatus#ACCEPTED}, as only accepted permissions can be connected.
     */
    @Nullable
    public ConnectionStatus connectionStatusOf(Permission permission) {
        if (!permission.status().isAccepted()) {
            return null;
        }

        var startTime = permission.startTime();
        if (startTime != null && startTime.isAfter(clock.instant())) {
            return ConnectionStatus.WAITING_FOR_CONNECTION;
        }

        return streamerManager.isConnected(permission.id())
                ? ConnectionStatus.CONNECTED
                : ConnectionStatus.NOT_CONNECTED;
    }

    /**
     * Creates the API representation of the passed permission, including its derived connection status.
     */
    public PermissionDto toDto(Permission permission) {
        return PermissionDto.from(permission, connectionStatusOf(permission));
    }
}

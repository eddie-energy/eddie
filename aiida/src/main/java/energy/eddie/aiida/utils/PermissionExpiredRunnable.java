// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.utils;

import energy.eddie.aiida.models.permission.ActivePermission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class PermissionExpiredRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionExpiredRunnable.class);
    private final ActivePermission permission;
    private final StreamerManager streamerManager;
    private final PermissionRepository repository;
    private final Instant expirationTime;
    private final Clock clock;

    /**
     * Create a new PermissionExpiredRunnable that when executed, instructs the {@link StreamerManager} to send the
     * {@link PermissionStatus#FULFILLED} status message to the EDDIE framework, stops streaming for the specified
     * permission and updates the database.
     */
    public PermissionExpiredRunnable(
            ActivePermission permission,
            StreamerManager streamerManager,
            PermissionRepository repository,
            Clock clock
    ) {
        this.permission = permission;
        this.streamerManager = streamerManager;
        this.repository = repository;
        this.clock = clock;
        this.expirationTime = permission.expirationTime();
    }

    @Override
    public void run() {
        var offsetMs = clock.instant().toEpochMilli() - expirationTime.toEpochMilli();

        LOGGER.atInfo()
              .addArgument(permission.id())
              .addArgument(offsetMs)
              .log("Will expire permission permission {}, running {} ms too late compared to target time (negative number would be too early)");

        var currentPermission = repository.findById(permission.id()).orElse(null);
        if (currentPermission == null) {
            LOGGER.warn("Permission {} no longer exists. Will NOT expire the permission", permission.id());
            return;
        }

        // safeguard if e.g. a revocation operation could not properly cancel this runnable before it runs
        if (!currentPermission.status().isActive()) {
            LOGGER.warn("Permission {} was modified, its status is {}. Will NOT expire the permission",
                        permission.id(), currentPermission.status());
            return;
        }

        var fulfilledMessage = new AiidaConnectionStatusMessageDto(permission.connectionId(),
                                                                   permission.dataNeed().dataNeedId(),
                                                                   clock.instant(),
                                                                   PermissionProcessStatus.FULFILLED,
                                                                   permission.id(),
                                                                   permission.eddieId()
        );

        currentPermission.setStatus(PermissionStatus.FULFILLED);
        repository.save(currentPermission);

        streamerManager.stopStreamer(fulfilledMessage);
    }
}

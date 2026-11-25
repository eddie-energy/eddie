// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.models.permission.AcceptedPermission;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.schemas.pmd.PermissionMarketDocumentFactory;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import jakarta.transaction.Transactional;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class PermissionScheduler implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionScheduler.class);
    private final Clock clock;
    private final TaskScheduler scheduler;
    private final PermissionRepository repository;
    /**
     * The same map can be used to keep track of futures from scheduled permission starts and expirations because there
     * must never be a runnable for start and expiration scheduled for the same permissionId at the same time.
     */
    private final ConcurrentMap<UUID, ScheduledFuture<?>> permissionFutures;
    private final StreamerManager streamerManager;
    private final PermissionMarketDocumentFactory permissionMarketDocumentFactory;
    private final FailedToSendRepository failedToSendRepository;

    public PermissionScheduler(
            Clock clock,
            TaskScheduler scheduler,
            PermissionRepository repository,
            ConcurrentMap<UUID, ScheduledFuture<?>> permissionFutures,
            StreamerManager streamerManager,
            PermissionMarketDocumentFactory permissionMarketDocumentFactory,
            FailedToSendRepository failedToSendRepository
    ) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.repository = repository;
        this.permissionFutures = permissionFutures;
        this.streamerManager = streamerManager;
        this.permissionMarketDocumentFactory = permissionMarketDocumentFactory;
        this.failedToSendRepository = failedToSendRepository;
    }

    /**
     * Gets all accepted permissions with MQTT credentials from the database and checks if they have expired.
     * If not, streaming is resumed, otherwise their database entry will be updated accordingly and their unsent
     * messages are deleted.
     * This is done when a {@link ContextRefreshedEvent} is received,
     * which ensures that all beans are started and the database is set up correctly.
     */
    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // fields of permissions are loaded eagerly to avoid n+1 select problem in loop
        var acceptedPermissions = repository.findByMqttStreamingConfigIsNotNullAndStatus(PermissionStatus.ACCEPTED);
        LOGGER.info(
                "Fetched {} permission from database that should resume streaming or be updated if expired.",
                acceptedPermissions.size());

        var now = clock.instant();
        for (Permission permission : acceptedPermissions) {
            try {
                var acceptedPermission = AcceptedPermission.from(permission);
                if (acceptedPermission.expirationTime().isAfter(now)) {
                    scheduleOrStart(acceptedPermission);
                } else {
                    LOGGER.info(
                            "Permission {} has expired but AIIDA was not running at that time, will expire it now",
                            permission.id());
                    permission.setStatus(PermissionStatus.FULFILLED);
                    failedToSendRepository.deleteAllByPermissionId(permission.id());
                }
            } catch (IllegalStateException exception) {
                // don't throw exception so other permissions are not skipped
                LOGGER.error("Cannot resume streaming for permission {}", permission.id(), exception);
            }
        }
    }

    /**
     * If the start date of the permission is in the past or now, the streaming is directly started, otherwise a
     * Runnable will be scheduled to start the permission once its start time has been reached. The status of the
     * permission is not changed, its connection state is derived at runtime by {@link ConnectionStatusService}.
     */
    public void scheduleOrStart(AcceptedPermission permission) {
        var startTime = permission.startTime();

        var now = Instant.now(clock);
        if (startTime.isAfter(now)) {
            schedulePermissionStart(permission);
        } else {
            startPermission(permission);
        }
    }

    /**
     * Removes any scheduled start or expiration runnable for the specified ID. E.g. when the permission is revoked or
     * terminated.
     */
    public void removePermission(UUID permissionId) {
        var future = permissionFutures.remove(permissionId);
        if (future != null) {
            LOGGER.info("Cancelling permissionExpiration/permissionStart future for permission '{}'", permissionId);
            future.cancel(true);
        }
    }

    private void schedulePermissionStart(AcceptedPermission permission) {
        var startTime = permission.startTime();
        var permissionId = permission.id();

        LOGGER.info("Scheduling permission start for permission '{}'.", permissionId);

        var future = scheduler.schedule(() -> startPermission(permission), startTime);
        permissionFutures.put(permissionId, future);
    }

    private void startPermission(AcceptedPermission permission) {
        LOGGER.info("Starting permission '{}' owned by EDDIE framework '{}'",
                    permission.id(),
                    permission.eddieId());

        try {
            streamerManager.createNewStreamer(permission);
        } catch (MqttException exception) {
            LOGGER.atError()
                  .addArgument(permission.id())
                  .addArgument(permission.eddieId())
                  .setCause(exception)
                  .log("Failed to start streaming for permission '{}' owned by EDDIE framework '{}' because the MqttClient could not be created");
        }
        schedulePermissionExpirationRunnable(permission);
    }

    private void schedulePermissionExpirationRunnable(AcceptedPermission permission) {
        var expirationTime = permission.expirationTime();
        var permissionId = permission.id();
        LOGGER.info("Will schedule a PermissionExpirationRunnable for permission '{}' to run at  {}",
                    permissionId,
                    expirationTime);

        var expirationRunnable = new PermissionExpiredRunnable(permission,
                                                               streamerManager,
                                                               permissionMarketDocumentFactory,
                                                               repository,
                                                               clock);
        ScheduledFuture<?> future = scheduler.schedule(expirationRunnable, expirationTime);

        permissionFutures.put(permissionId, future);
    }
}

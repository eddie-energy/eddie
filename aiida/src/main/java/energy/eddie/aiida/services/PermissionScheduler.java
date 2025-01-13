package energy.eddie.aiida.services;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import static energy.eddie.aiida.models.permission.PermissionStatus.WAITING_FOR_START;
import static java.util.Objects.requireNonNull;

@Service
public class PermissionScheduler {
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

    public PermissionScheduler(
            Clock clock,
            TaskScheduler scheduler,
            PermissionRepository repository,
            ConcurrentMap<UUID, ScheduledFuture<?>> permissionFutures,
            StreamerManager streamerManager
    ) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.repository = repository;
        this.permissionFutures = permissionFutures;
        this.streamerManager = streamerManager;
    }

    /**
     * If the start date of the permission is in the past or now, the streaming is directly started, otherwise a
     * Runnable will be scheduled to start the permission once its start time has been reached.
     *
     * @return The updated permission object. It is already persisted in the database.
     */
    public Permission scheduleOrStart(Permission permission) {
        var startTime = requireNonNull(permission.startTime());

        var now = Instant.now(clock);
        if (startTime.isAfter(now)) {
            return schedulePermissionStart(permission);
        } else {
            return startPermission(permission);
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

    private Permission schedulePermissionStart(Permission permission) {
        var startTime = requireNonNull(permission.startTime());
        var permissionId = permission.permissionId();

        LOGGER.info("Scheduling permission start for permission '{}'.", permissionId);
        permission.setStatus(WAITING_FOR_START);

        var future = scheduler.schedule(() -> startPermission(permission), startTime);
        permissionFutures.put(permissionId, future);

        return repository.save(permission);
    }

    private Permission startPermission(Permission permission) {
        LOGGER.info("Starting permission '{}' owned by EDDIE framework '{}'",
                    permission.permissionId(),
                    permission.eddieId());

        try {
            streamerManager.createNewStreamer(permission);
            schedulePermissionExpirationRunnable(permission);
            permission.setStatus(PermissionStatus.STREAMING_DATA);
        } catch (MqttException exception) {
            LOGGER.atError()
                  .addArgument(permission.permissionId())
                  .addArgument(permission.eddieId())
                  .setCause(exception)
                  .log("Failed to start streaming for permission '{}' owned by EDDIE framework '{}' because the MqttClient could not be created");

            permission.setStatus(PermissionStatus.FAILED_TO_START);
        }
        return repository.save(permission);
    }

    private void schedulePermissionExpirationRunnable(Permission permission) {
        var expirationTime = requireNonNull(permission.expirationTime());
        var permissionId = permission.permissionId();
        LOGGER.info("Will schedule a PermissionExpirationRunnable for permission '{}' to run at  {}",
                    permissionId,
                    expirationTime);

        var expirationRunnable = new PermissionExpiredRunnable(permission, streamerManager, repository, clock);
        ScheduledFuture<?> future = scheduler.schedule(expirationRunnable, expirationTime);

        permissionFutures.put(permissionId, future);
    }
}

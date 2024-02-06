package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.errors.InvalidPermissionRevocationException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.errors.PermissionStartFailedException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import static energy.eddie.aiida.models.permission.PermissionStatus.*;

@Service
public class PermissionService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    /**
     * The same map can be used for futures from scheduled permission starts and expirations. There will never be a
     * runnable for start and expiration scheduled for the same permissionId at the same time.
     */
    private final ConcurrentMap<String, ScheduledFuture<?>> permissionFutures;
    private final PermissionRepository repository;
    private final Clock clock;
    private final StreamerManager streamerManager;
    private final TaskScheduler scheduler;

    @Autowired
    public PermissionService(PermissionRepository repository, Clock clock, StreamerManager streamerManager,
                             TaskScheduler scheduler, ConcurrentMap<String, ScheduledFuture<?>> permissionFutures) {
        this.repository = repository;
        this.clock = clock;
        this.streamerManager = streamerManager;
        this.scheduler = scheduler;
        this.permissionFutures = permissionFutures;

        streamerManager.terminationRequestsFlux().subscribe(this::terminationRequestReceived);
    }

    private void terminationRequestReceived(String permissionId) {
        LOGGER.info("Will handle termination request for permission {}", permissionId);

        Permission permission = findById(permissionId);

        var future = permissionFutures.get(permissionId);
        if (future != null) {
            LOGGER.info("Cancelling permissionExpiration/permissionStart future for permission {}", permissionId);
            future.cancel(true);
        }

        var previousStatus = permission.status();

        Instant terminateTime = clock.instant();
        permission.revokeTime(terminateTime);
        permission.updateStatus(TERMINATED);
        repository.save(permission);

        if (previousStatus == WAITING_FOR_START)
            // if permission has never sent data, we also don't send the TERMINATED status message
            return;

        try {
            var terminated = new ConnectionStatusMessage(permission.connectionId(), permission.dataNeedId(), terminateTime, TERMINATED, permissionId);
            streamerManager.sendConnectionStatusMessageForPermission(terminated, permissionId);
        } catch (ConnectionStatusMessageSendFailedException ex) {
            LOGGER.error("Failed to send TERMINATED status message for permission {}", permissionId, ex);
        }

        streamerManager.stopStreamer(permissionId);
    }

    /**
     * Saves a new permission in the database and sends the {@link PermissionStatus#ACCEPTED} status message to
     * the EDDIE framework. If any error occurs, the permission is not saved in the database.
     * If the permission's startTime is in the future, it will be scheduled to start then, and the status message will
     * also just be sent when the permission's start time has been reached.
     *
     * @param dto Data transfer object containing the information for the new permission.
     * @return Permission object as returned by the database.
     * @throws PermissionStartFailedException If the permission couldn't be started.
     */
    public Permission setupNewPermission(PermissionDto dto) throws PermissionStartFailedException {
        Permission newPermission = new Permission(dto.permissionId(), dto.serviceName(), dto.dataNeedId(),
                dto.startTime(), dto.expirationTime(), dto.grantTime(), dto.connectionId(),
                dto.requestedCodes(), dto.kafkaStreamingConfig());
        newPermission = repository.save(newPermission);

        var now = Instant.now(clock);
        if (now.isAfter(newPermission.startTime())) {
            return startPermission(newPermission);
        } else {
            return schedulePermissionStart(newPermission);
        }
    }

    private Permission schedulePermissionStart(Permission permission) {
        LOGGER.info("Scheduling permission start for permission {}", permission.permissionId());
        permission.updateStatus(WAITING_FOR_START);
        Permission finalPermission = repository.save(permission);

        var future = scheduler.schedule(() -> permissionStartRunnable(finalPermission), finalPermission.startTime());

        permissionFutures.put(finalPermission.permissionId(), future);

        return finalPermission;
    }

    private void permissionStartRunnable(Permission permission) {
        try {
            startPermission(permission);
        } catch (PermissionStartFailedException exception) {
            // status is already updated, nothing left to process here
            LOGGER.error("Scheduled start of permission {} failed", permission.permissionId(), exception);
        }
    }

    private Permission startPermission(Permission permission) throws PermissionStartFailedException {
        LOGGER.info("Starting permission {}", permission.permissionId());
        var acceptedMessage = new ConnectionStatusMessage(permission.connectionId(), permission.dataNeedId(),
                clock.instant(), ACCEPTED, permission.permissionId());

        try {
            streamerManager.createNewStreamerForPermission(permission);
            streamerManager.sendConnectionStatusMessageForPermission(acceptedMessage, permission.permissionId());

            schedulePermissionExpirationRunnable(permission);

            permission.updateStatus(PermissionStatus.STREAMING_DATA);
            return repository.save(permission);
        } catch (ConnectionStatusMessageSendFailedException exception) {
            LOGGER.error("Failed to start permission {}", permission.permissionId(), exception);
            permission.updateStatus(FAILED_TO_START);
            permission = repository.save(permission);
            throw new PermissionStartFailedException(permission);
        }
    }

    private void schedulePermissionExpirationRunnable(Permission permission) {
        LOGGER.info("Will schedule a PermissionExpirationRunnable for permission {} to run at  {}", permission.permissionId(), permission.expirationTime());

        Sinks.One<String> expirationSink = Sinks.one();
        expirationSink.asMono().subscribe(this::expirePermission);
        var expirationRunnable = new PermissionExpiredRunnable(permission.permissionId(),
                permission.expirationTime(), expirationSink);
        ScheduledFuture<?> future = scheduler.schedule(expirationRunnable, permission.expirationTime());

        permissionFutures.put(permission.permissionId(), future);
    }

    /**
     * Revokes the specified permission by updating its status and records the timestamp and persisting the changes.
     * If an error during shutdown of the AiidaStreamer or sending of the {@link ConnectionStatusMessage} occurs,
     * they are logged but not propagated to the caller.
     *
     * @param permissionId ID of the permission that should be revoked.
     * @return Updated permission object that has been persisted.
     * @throws PermissionNotFoundException          In case no permission with the specified ID can be found.
     * @throws InvalidPermissionRevocationException In case the permission has a status that makes it not eligible for revocation.
     */
    public Permission revokePermission(String permissionId) throws PermissionNotFoundException, InvalidPermissionRevocationException {
        LOGGER.info("Got request to revoke permission with id {}", permissionId);

        var permission = findById(permissionId);

        if (!isEligibleForRevocation(permission))
            throw new InvalidPermissionRevocationException(permissionId);

        var future = permissionFutures.get(permissionId);
        if (future != null) {
            LOGGER.info("Cancelling permissionExpiration/permissionStart future for permission {}", permissionId);
            future.cancel(true);
        }

        var previousStatus = permission.status();

        Instant revocationTime = clock.instant();
        permission.updateStatus(REVOKED);
        permission.revokeTime(revocationTime);
        permission = repository.save(permission);

        if (previousStatus == WAITING_FOR_START)
            // if permission has never sent data, we also don't send the REVOKED status messages
            return permission;

        var revocationReceivedMessage = new ConnectionStatusMessage(permission.connectionId(), permission.dataNeedId(), revocationTime, REVOCATION_RECEIVED, permissionId);
        var revokeMessage = new ConnectionStatusMessage(permission.connectionId(), permission.dataNeedId(), revocationTime, REVOKED, permissionId);

        try {
            streamerManager.sendConnectionStatusMessageForPermission(revocationReceivedMessage, permissionId);
            streamerManager.sendConnectionStatusMessageForPermission(revokeMessage, permissionId);
            streamerManager.stopStreamer(permissionId);
        } catch (ConnectionStatusMessageSendFailedException | IllegalArgumentException ex) {
            LOGGER.error("Error while sending connection status messages while revoking permission {}", permissionId, ex);
        }

        return permission;
    }

    /**
     * Returns all permission objects that are persisted, sorted by their grantTime descending.
     *
     * @return A list of permissions, sorted by grantTime descending, i.e. the permission with the newest grantTime is the first item.
     */
    public List<Permission> getAllPermissionsSortedByGrantTime() {
        return repository.findAllByOrderByGrantTimeDesc();
    }

    /**
     * Returns the permission with the specified ID.
     *
     * @param permissionId ID of the permission to be returned.
     * @return The permission object with the specified ID.
     * @throws PermissionNotFoundException In case no permission with the specified ID can be found.
     */
    public Permission findById(String permissionId) throws PermissionNotFoundException {
        return repository.findById(permissionId).orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    /**
     * Sends the {@link PermissionStatus#TIME_LIMIT} status message to the EDDIE framework for the permission with
     * {@code permissionId} , stops the associated streamer, sets the permission's status to TIME_LIMIT and
     * updates the database.
     *
     * @param permissionId ID of the permission which has reached its expiration time.
     */
    private void expirePermission(String permissionId) {
        LOGGER.info("Will expire permission with id {}", permissionId);
        Permission permission;
        try {
            permission = findById(permissionId);
        } catch (PermissionNotFoundException ex) {
            LOGGER.error("No permission with id {} found in database, but was requested to expire it.", permissionId);
            return;
        }

        if (!(permission.status() == PermissionStatus.ACCEPTED ||
                permission.status() == PermissionStatus.WAITING_FOR_START ||
                permission.status() == PermissionStatus.STREAMING_DATA)) {
            LOGGER.warn("Permission {} was modified, its status is {}. Will NOT expire the permission", permissionId, permission.status());
            return;
        }

        if (permission.status() == PermissionStatus.ACCEPTED || permission.status() == PermissionStatus.WAITING_FOR_START) {
            LOGGER.warn("Permission {} has status {}, meaning it was never started. Will expire it anyway.", permissionId, permission.status());
        }

        var statusMessage = new ConnectionStatusMessage(permission.connectionId(), permission.dataNeedId(), clock.instant(), PermissionStatus.TIME_LIMIT, permissionId);

        try {
            streamerManager.sendConnectionStatusMessageForPermission(statusMessage, permissionId);
        } catch (ConnectionStatusMessageSendFailedException ex) {
            LOGGER.error("Error while sending TIME_LIMIT ConnectionStatusMessage", ex);
        }
        streamerManager.stopStreamer(permissionId);

        permission.updateStatus(PermissionStatus.TIME_LIMIT);
        repository.save(permission);
    }

    /**
     * Indicates whether the permission's current status allows it to be revoked.
     * The status needs to be one of the following, to be eligible for revocation: ACCEPTED, WAITING_FOR_START, STREAMING_DATA
     *
     * @param permission Permission to check.
     * @return True if the permission is eligible for revocation, false otherwise.
     */
    private boolean isEligibleForRevocation(Permission permission) {
        return switch (permission.status()) {
            case ACCEPTED, WAITING_FOR_START, STREAMING_DATA -> true;
            default -> false;
        };
    }

    /**
     * Gets all active permissions from the database and checks if they have expired.
     * If not, streaming is resumed, otherwise their database entry will be updated accordingly.
     * This is done when a {@link ContextRefreshedEvent} is received, which ensures that all beans are started
     * and the database is set up correctly.
     */
    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // fields of permissions are loaded eagerly to avoid n+1 select problem in loop
        List<Permission> allActivePermissions = repository.findAllActivePermissions();
        LOGGER.info("Fetched {} active permissions from database and will resume streaming or update them if they are expired.", allActivePermissions.size());

        for (Permission permission : allActivePermissions) {
            if (permission.expirationTime().isAfter(clock.instant())) {
                schedulePermissionExpirationRunnable(permission);
                streamerManager.createNewStreamerForPermission(permission);

                permission.updateStatus(PermissionStatus.STREAMING_DATA);
                repository.save(permission);
            } else {
                LOGGER.info("Permission {} has expired but AIIDA was not running at that time, will expire it now", permission.permissionId());
                permission.updateStatus(PermissionStatus.TIME_LIMIT);
                repository.save(permission);
            }
        }
    }

    /**
     * Use static nested class to ensure Spring will inject the ConcurrentHashMap only into the PermissionService and
     * it won't be shared with other classes.
     */
    @Configuration
    public static class ConcurrentHashMapConfig {
        @Bean
        // Spring's TaskScheduler only returns a ScheduledFuture<?>, so we have to use wildcards
        @SuppressWarnings("java:S1452")
        ConcurrentHashMap<String, ScheduledFuture<?>> expirationFutures() {
            return new ConcurrentHashMap<>();
        }
    }
}

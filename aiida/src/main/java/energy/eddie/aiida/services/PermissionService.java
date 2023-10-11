package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.errors.InvalidPermissionRevocationException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class PermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final ConcurrentMap<String, ScheduledFuture<?>> expirationFutures;
    private final PermissionRepository repository;
    private final Clock clock;
    private final StreamerManager streamerManager;
    private final TaskScheduler scheduler;

    @Autowired
    public PermissionService(PermissionRepository repository, Clock clock, StreamerManager streamerManager,
                             TaskScheduler scheduler, ConcurrentMap<String, ScheduledFuture<?>> expirationFutures) {
        this.repository = repository;
        this.clock = clock;
        this.streamerManager = streamerManager;
        this.scheduler = scheduler;
        this.expirationFutures = expirationFutures;
    }

    /**
     * Saves a new permission in the database and sends the {@link PermissionStatus#ACCEPTED} status to the EDDIE
     * framework. If any error occurs, the permission is not saved in the database.
     *
     * @param dto Data transfer object containing the information for the new permission.
     * @return Permission object as returned by the database (i.e. with a permissionId).
     */
    @Transactional(rollbackFor = ConnectionStatusMessageSendFailedException.class)
    public Permission setupNewPermission(PermissionDto dto) throws ConnectionStatusMessageSendFailedException {
        Permission newPermission = new Permission(dto.serviceName(), dto.startTime(), dto.expirationTime(),
                dto.grantTime(), dto.connectionId(), dto.requestedCodes(), dto.kafkaStreamingConfig());
        newPermission = repository.save(newPermission);

        var acceptedMessage = new ConnectionStatusMessage(newPermission.connectionId(),
                clock.instant(), PermissionStatus.ACCEPTED);
        streamerManager.createNewStreamerForPermission(newPermission);
        streamerManager.sendConnectionStatusMessageForPermission(acceptedMessage, newPermission.permissionId());

        Sinks.One<String> sink = Sinks.one();
        sink.asMono().subscribe(this::expirePermission);

        var expirationRunnable = new PermissionExpiredRunnable(newPermission.permissionId(),
                newPermission.expirationTime(), sink);
        ScheduledFuture<?> future = scheduler.schedule(expirationRunnable, newPermission.expirationTime());

        expirationFutures.put(newPermission.permissionId(), future);

        return newPermission;
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

        var future = expirationFutures.get(permissionId);
        if (future != null) {
            LOGGER.info("Cancelling expiration future for permission {}", permissionId);
            future.cancel(true);
        }

        Instant revocationTime = clock.instant();
        var revocationReceivedMessage = new ConnectionStatusMessage(permission.connectionId(), revocationTime, PermissionStatus.REVOCATION_RECEIVED);
        var revokeMessage = new ConnectionStatusMessage(permission.connectionId(), revocationTime, PermissionStatus.REVOKED);

        try {
            streamerManager.sendConnectionStatusMessageForPermission(revocationReceivedMessage, permissionId);
            streamerManager.sendConnectionStatusMessageForPermission(revokeMessage, permissionId);
            streamerManager.stopStreamer(permissionId);
        } catch (ConnectionStatusMessageSendFailedException | IllegalArgumentException ex) {
            LOGGER.error("Error while sending connection status messages while revoking permission {}", permissionId, ex);
        }

        permission.updateStatus(PermissionStatus.REVOKED);
        permission.revokeTime(revocationTime);

        return repository.save(permission);
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

        var statusMessage = new ConnectionStatusMessage(permission.connectionId(), clock.instant(), PermissionStatus.TIME_LIMIT);

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

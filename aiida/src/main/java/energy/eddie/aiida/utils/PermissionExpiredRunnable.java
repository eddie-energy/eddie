package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class PermissionExpiredRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionExpiredRunnable.class);
    private final Permission permission;
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
            Permission permission,
            StreamerManager streamerManager,
            PermissionRepository repository,
            Clock clock
    ) {
        this.permission = permission;
        this.streamerManager = streamerManager;
        this.repository = repository;
        this.clock = clock;
        this.expirationTime = requireNonNull(permission.expirationTime());
    }

    @Override
    public void run() {
        var offsetMs = clock.instant().toEpochMilli() - expirationTime.toEpochMilli();

        LOGGER.atInfo()
              .addArgument(permission.permissionId())
              .addArgument(offsetMs)
              .log("Will expire permission permission {}, running {} ms too late compared to target time (negative number would be too early)");

        // safeguard if e.g. a revocation operation could not properly cancel this runnable before it runs
        if (!(permission.status() == PermissionStatus.ACCEPTED ||
              permission.status() == PermissionStatus.WAITING_FOR_START ||
              permission.status() == PermissionStatus.STREAMING_DATA)) {
            LOGGER.warn("Permission {} was modified, its status is {}. Will NOT expire the permission",
                        permission.permissionId(), permission.status());
            return;
        }

        if (permission.status() == PermissionStatus.ACCEPTED || permission.status() == PermissionStatus.WAITING_FOR_START) {
            LOGGER.warn("Permission {} has status {}, meaning it was never started. Will expire it anyway.",
                        permission.permissionId(), permission.status());
        }

        var dataNeedId = requireNonNull(permission.dataNeed()).dataNeedId();
        var connectionId = requireNonNull(permission.connectionId());
        var fulfilledMessage = new ConnectionStatusMessage(connectionId,
                                                           dataNeedId,
                                                           clock.instant(),
                                                           PermissionStatus.FULFILLED,
                                                           permission.permissionId(),
                                                           permission.eddieId()
        );

        permission.setStatus(PermissionStatus.FULFILLED);
        repository.save(permission);

        streamerManager.stopStreamer(fulfilledMessage);
    }
}

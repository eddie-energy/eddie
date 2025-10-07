package energy.eddie.aiida.services.cleanup;

import energy.eddie.aiida.config.cleanup.CleanupEntity;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class TimeBasedCleanupService implements EntityCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeBasedCleanupService.class);

    private final CleanupEntity cleanupEntity;
    private final Duration retention;
    private final ExpiredEntityDeleter expiredEntityDeleter;

    protected TimeBasedCleanupService(
            CleanupEntity cleanupEntity,
            Duration retention,
            ExpiredEntityDeleter expiredEntityDeleter
    ) {
        this.cleanupEntity = cleanupEntity;
        this.retention = retention;
        this.expiredEntityDeleter = expiredEntityDeleter;
    }

    @Override
    @Transactional
    public void deleteExpiredEntities() {
        var threshold = Instant.now().minus(retention);
        var deletedEntities = expiredEntityDeleter.deleteEntitiesOlderThan(threshold);

        LOGGER.debug("Deleting {} expired entities ({}) older than {} (retention: {})",
                     deletedEntities,
                     cleanupEntity,
                     LocalDateTime.ofInstant(threshold, ZoneId.systemDefault()),
                     retention);
    }
}

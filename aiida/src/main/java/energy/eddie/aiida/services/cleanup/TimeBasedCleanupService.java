package energy.eddie.aiida.services.cleanup;

import energy.eddie.aiida.config.cleanup.CleanupEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public abstract class TimeBasedCleanupService implements EntityCleanupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeBasedCleanupService.class);

    protected static final int BATCH_SIZE = 1_000;

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
    public int deleteExpiredEntities() {
        final Instant threshold = Instant.now().minus(retention);

        LOGGER.debug("Starting cleanup for {} (retention: {}, batch size: {})", cleanupEntity, retention, BATCH_SIZE);

        var totalDeleted = 0;
        while (true) {
            try {
                var deletedInBatch = expiredEntityDeleter.deleteOldestByTimestampBefore(threshold, BATCH_SIZE);
                if (deletedInBatch == 0) {
                    break;
                }

                totalDeleted += deletedInBatch;

                LOGGER.debug("Deleted {} expired entities for {} in current batch", deletedInBatch, cleanupEntity);
            } catch (Exception ex) {
                LOGGER.error("Cleanup aborted for {} after deleting {} entities", cleanupEntity, totalDeleted, ex);
                break;
            }
        }

        LOGGER.debug("Finished cleanup for {} – deleted {} entities older than {}",
                    cleanupEntity,
                    totalDeleted,
                    threshold);
        return totalDeleted;
    }
}

package energy.eddie.aiida.services.cleanup;

import java.time.Instant;

@FunctionalInterface
public interface ExpiredEntityDeleter {
    int deleteOldestByTimestampBefore(Instant threshold, int limit);
}

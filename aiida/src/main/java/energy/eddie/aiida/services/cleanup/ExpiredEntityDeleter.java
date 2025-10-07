package energy.eddie.aiida.services.cleanup;

import java.time.Instant;

@FunctionalInterface
public interface ExpiredEntityDeleter {
    long deleteEntitiesOlderThan(Instant threshold);
}

package energy.eddie.aiida.services.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CleanupScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupScheduler.class);

    private final List<EntityCleanupService> services;

    public CleanupScheduler(List<EntityCleanupService> services) {
        this.services = services;
    }

    @Scheduled(fixedDelayString = "#{@cleanupConfiguration.cleanupInterval.toMillis()}")
    public void cleanup() {
        LOGGER.info("Starting scheduled cleanup run for expired entities");

        var totalDeleted = 0;
        for (var service : services) {
            totalDeleted += service.deleteExpiredEntities();
        }

        LOGGER.info("Cleanup run completed – deleted a total of {} expired entities", totalDeleted);
    }
}

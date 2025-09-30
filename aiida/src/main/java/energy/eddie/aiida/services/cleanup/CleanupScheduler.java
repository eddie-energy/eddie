package energy.eddie.aiida.services.cleanup;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CleanupScheduler {
    private final List<EntityCleanupService> services;

    public CleanupScheduler(List<EntityCleanupService> services) {
        this.services = services;
    }

    @Scheduled(fixedDelayString = "#{@cleanupConfiguration.cleanupInterval.toMillis()}")
    public void cleanup() {
        services.forEach(EntityCleanupService::deleteExpiredEntities);
    }
}

package energy.eddie.aiida.services;

import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupEntity;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.services.cleanup.TimeBasedCleanupService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class FailedToSendService extends TimeBasedCleanupService {
    public FailedToSendService(CleanupConfiguration cleanupConfiguration, FailedToSendRepository repository) {
        super(CleanupEntity.FAILED_TO_SEND_ENTITY,
              Objects.requireNonNull(cleanupConfiguration.entities().get(CleanupEntity.FAILED_TO_SEND_ENTITY))
                     .retention(),
              repository::deleteFailedToSendEntitiesByCreatedAtBefore);
    }
}

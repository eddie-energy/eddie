// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.cleanup.entities;

import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupEntity;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.services.cleanup.TimeBasedCleanupService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class FailedToSendCleanupService extends TimeBasedCleanupService {
    public FailedToSendCleanupService(CleanupConfiguration cleanupConfiguration, FailedToSendRepository repository) {
        super(CleanupEntity.FAILED_TO_SEND_ENTITY,
              Objects.requireNonNull(cleanupConfiguration.entities().get(CleanupEntity.FAILED_TO_SEND_ENTITY))
                     .retention(),
              repository::deleteOldestByCreatedAtBefore);
    }
}

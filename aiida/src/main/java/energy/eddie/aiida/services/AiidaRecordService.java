// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupEntity;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.services.cleanup.TimeBasedCleanupService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AiidaRecordService extends TimeBasedCleanupService {
    public AiidaRecordService(CleanupConfiguration cleanupConfiguration, AiidaRecordRepository repository) {
        super(CleanupEntity.AIIDA_RECORD,
              Objects.requireNonNull(cleanupConfiguration.entities().get(CleanupEntity.AIIDA_RECORD)).retention(),
              repository::deleteOldestByTimestampBefore);
    }
}

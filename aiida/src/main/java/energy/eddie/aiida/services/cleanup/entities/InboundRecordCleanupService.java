// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.cleanup.entities;

import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupEntity;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.services.cleanup.TimeBasedCleanupService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class InboundRecordCleanupService extends TimeBasedCleanupService {
    public InboundRecordCleanupService(CleanupConfiguration cleanupConfiguration, InboundRecordRepository repository) {
        super(CleanupEntity.INBOUND_RECORD,
              Objects.requireNonNull(cleanupConfiguration.entities().get(CleanupEntity.INBOUND_RECORD)).retention(),
              repository::deleteOldestByTimestampBefore);
    }
}

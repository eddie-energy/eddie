// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupEntity;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.services.cleanup.TimeBasedCleanupService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class InboundRecordService extends TimeBasedCleanupService {
    public InboundRecordService(CleanupConfiguration cleanupConfiguration, InboundRecordRepository repository) {
        super(CleanupEntity.INBOUND_RECORD,
              Objects.requireNonNull(cleanupConfiguration.entities().get(CleanupEntity.INBOUND_RECORD)).retention(),
              repository::deleteOldestByTimestampBefore);
    }
}

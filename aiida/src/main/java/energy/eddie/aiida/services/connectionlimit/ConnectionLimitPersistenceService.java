// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class ConnectionLimitPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitPersistenceService.class);

    private final ConnectionLimitRepository connectionLimitRepository;
    private final TransactionTemplate transactionTemplate;

    public ConnectionLimitPersistenceService(
            ConnectionLimitRepository connectionLimitRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.connectionLimitRepository = connectionLimitRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public Optional<Integer> findMaxRevisionNumberByMrid(String mrid) {
        return connectionLimitRepository.findMaxRevisionNumberByMrid(mrid);
    }

    public void replaceByMrid(String mrid, List<ConnectionLimit> connectionLimits) {
        transactionTemplate.executeWithoutResult(status -> {
            connectionLimitRepository.deleteByMrid(mrid);
            connectionLimitRepository.saveAll(connectionLimits);
        });
    }

    public int upsertByCreatedAt(List<ConnectionLimit> connectionLimits) {
        int persistedCount = 0;
        for (var incoming : connectionLimits) {
            var existing = connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(incoming.permissionId(),
                                                                                               incoming.meterId(),
                                                                                               incoming.intervalStart(),
                                                                                               incoming.intervalEnd());

            if (existing.isEmpty() || incoming.createdAt().isAfter(existing.get())) {
                connectionLimitRepository.save(incoming);
                persistedCount++;
            } else {
                LOGGER.warn(
                        "Rejected connection limit update for permission {}, meter {}, interval [{} - {}]: createdDateTime {} is not newer than existing createdDateTime {}",
                        incoming.permissionId(),
                        incoming.meterId(),
                        incoming.intervalStart(),
                        incoming.intervalEnd(),
                        incoming.createdAt(),
                        existing.get());
            }
        }

        return persistedCount;
    }
}

// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.AiidaRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiidaRecordRepository extends JpaRepository<AiidaRecord, Long> {
    Optional<AiidaRecord> findFirstByDataSourceIdOrderByIdDesc(UUID dataSourceId);
    List<AiidaRecord> findByDataSourceIdOrderByTimestampDesc(UUID dataSourceId, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = """
            WITH to_delete AS (
                SELECT id
                FROM aiida_record
                WHERE timestamp < :threshold
                ORDER BY timestamp
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM aiida_record
            WHERE id IN (SELECT id FROM to_delete)
            """,
            nativeQuery = true
    )
    int deleteOldestByTimestampBefore(
            @Param("threshold") Instant threshold,
            @Param("limit") int limit
    );
}

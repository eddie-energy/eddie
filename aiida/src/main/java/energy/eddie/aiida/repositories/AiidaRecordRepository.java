// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.monitoring.PowerSample;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.api.agnostic.aiida.ObisCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AiidaRecordRepository extends JpaRepository<AiidaRecord, Long> {
    Optional<AiidaRecord> findFirstByDataSourceIdOrderByIdDesc(UUID dataSourceId);
    List<AiidaRecord> findByDataSourceIdOrderByTimestampDesc(UUID dataSourceId, Pageable pageable);

    @Query("""
            SELECT r.id, r.timestamp, v.dataTag, v.value, v.unitOfMeasurement
            FROM AiidaRecord r
            JOIN r.aiidaRecordValues v
            WHERE r.dataSource.id = :dataSourceId
              AND r.timestamp >= :from
              AND r.timestamp <= :to
              AND v.dataTag IN :dataTags
            ORDER BY r.timestamp, r.id
            """)
    List<PowerSample> findPowerSamplesByDataSourceId(
            UUID dataSourceId,
            Instant from,
            Instant to,
            Set<ObisCode> dataTags
    );

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

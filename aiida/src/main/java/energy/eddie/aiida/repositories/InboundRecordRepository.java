package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.InboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {
    Optional<InboundRecord> findTopByDataSourceIdOrderByTimestampDesc(UUID dataSourceId);

    @Transactional
    @Modifying
    @Query(value = """
            WITH to_delete AS (
                SELECT id
                FROM inbound_record
                WHERE timestamp < :threshold
                ORDER BY timestamp
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM inbound_record
            WHERE id IN (SELECT id FROM to_delete)
            """,
            nativeQuery = true
    )
    int deleteOldestByTimestampBefore(
            @Param("threshold") Instant threshold,
            @Param("limit") int limit
    );
}

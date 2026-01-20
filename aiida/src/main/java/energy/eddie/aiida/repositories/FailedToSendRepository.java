package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.FailedToSendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FailedToSendRepository extends JpaRepository<FailedToSendEntity, Long> {
    List<FailedToSendEntity> findAllByPermissionId(UUID permissionId);

    void deleteAllByPermissionId(UUID permissionId);

    @Transactional
    @Modifying
    @Query(value = """
            WITH to_delete AS (
                SELECT id
                FROM failed_to_send_entity
                WHERE created_at < :threshold
                ORDER BY created_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM failed_to_send_entity
            WHERE id IN (SELECT id FROM to_delete)
            """,
            nativeQuery = true
    )
    int deleteOldestByCreatedAtBefore(
            @Param("threshold") Instant threshold,
            @Param("limit") int limit
    );
}

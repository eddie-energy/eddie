package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.FailedToSendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FailedToSendRepository extends JpaRepository<FailedToSendEntity, Long> {
    List<FailedToSendEntity> findAllByPermissionId(UUID permissionId);

    void deleteAllByPermissionId(UUID permissionId);

    long deleteFailedToSendEntitiesByCreatedAtBefore(Instant threshold);
}

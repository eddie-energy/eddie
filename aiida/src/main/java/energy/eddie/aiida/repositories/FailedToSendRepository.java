package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.FailedToSendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FailedToSendRepository extends JpaRepository<FailedToSendEntity, Integer> {
    List<FailedToSendEntity> findAllByPermissionId(UUID permissionId);

    void deleteAllByPermissionId(UUID permissionId);
}

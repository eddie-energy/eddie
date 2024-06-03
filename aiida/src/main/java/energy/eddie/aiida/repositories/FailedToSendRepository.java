package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.FailedToSendEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedToSendRepository extends JpaRepository<FailedToSendEntity, Integer> {
    List<FailedToSendEntity> findAllByPermissionId(String permissionId);

    void deleteAllByPermissionId(String permissionId);
}

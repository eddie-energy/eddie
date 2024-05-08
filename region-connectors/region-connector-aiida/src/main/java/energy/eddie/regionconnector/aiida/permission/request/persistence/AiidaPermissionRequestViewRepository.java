package energy.eddie.regionconnector.aiida.permission.request.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository for selecting the aggregate permission request by using the SQL view.
 */
@Repository
@Transactional(readOnly = true)
public interface AiidaPermissionRequestViewRepository extends PermissionRequestRepository<AiidaPermissionRequest>, org.springframework.data.repository.Repository<AiidaPermissionRequest, String> {
    Optional<AiidaPermissionRequest> findById(String permissionId);

    @Override
    default void save(AiidaPermissionRequest request) {
        throw new UnsupportedOperationException("Not supported by this repository as it is just reading a database view");
    }

    @Override
    default Optional<AiidaPermissionRequest> findByPermissionId(String permissionId) {
        return findById(permissionId);
    }

    @Override
    default AiidaPermissionRequest getByPermissionId(String permissionId) {
        return findById(permissionId).orElseThrow(EntityNotFoundException::new);
    }
}

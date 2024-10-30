package energy.eddie.regionconnector.aiida.permission.request.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for selecting the aggregate permission request by using the SQL view.
 */
@Repository
@Transactional(readOnly = true)
public interface AiidaPermissionRequestViewRepository extends
        PermissionRequestRepository<AiidaPermissionRequest>,
        org.springframework.data.repository.Repository<AiidaPermissionRequest, String>,
        StalePermissionRequestRepository<AiidaPermissionRequest> {
    @Override
    default void save(AiidaPermissionRequest request) {
        throw new UnsupportedOperationException("Not supported by this repository as it is just reading a database view");
    }

    @Override
    default Optional<AiidaPermissionRequest> findByPermissionId(String permissionId) {
        return findById(permissionId);
    }

    Optional<AiidaPermissionRequest> findById(String permissionId);

    @Override
    default AiidaPermissionRequest getByPermissionId(String permissionId) {
        return findById(permissionId).orElseThrow(EntityNotFoundException::new);
    }

    @Query(
            value = "SELECT permission_id, status, connection_id, data_need_id, permission_start, permission_end, termination_topic, created, mqtt_username, message " +
                    "FROM aiida.aiida_permission_request_view WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<AiidaPermissionRequest> findStalePermissionRequests(@Param("hours") int duration);
}

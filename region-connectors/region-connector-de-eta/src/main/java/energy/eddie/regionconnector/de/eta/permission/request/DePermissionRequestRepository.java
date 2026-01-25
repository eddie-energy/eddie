package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.process.model.persistence.FullPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DePermissionRequest.
 * Extends Spring Data Repository and FullPermissionRequestRepository to provide all necessary operations.
 */
@org.springframework.stereotype.Repository
public interface DePermissionRequestRepository extends
        Repository<DePermissionRequest, String>,
        FullPermissionRequestRepository<DePermissionRequest> {

    @Override
    Optional<DePermissionRequest> findByPermissionId(String permissionId);

    @Override
    List<DePermissionRequest> findByStatus(PermissionProcessStatus status);

    @Override
    @Query(
            value = "SELECT permission_id, data_source_connection_id, metering_point_id, " +
                    "data_start, data_end, granularity, energy_type, status, data_need_id, " +
                    "created, latest_meter_reading, message, cause " +
                    "FROM de_eta.eta_permission_request " +
                    "WHERE status IN ('REQUESTED', 'PENDING_CONSENT') " +
                    "AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    List<DePermissionRequest> findStalePermissionRequests(@Param("hours") int stalenessDuration);

    void deleteAll();
}

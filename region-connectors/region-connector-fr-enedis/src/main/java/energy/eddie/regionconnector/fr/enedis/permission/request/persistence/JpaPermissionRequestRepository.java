package energy.eddie.regionconnector.fr.enedis.permission.request.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Should only be used by {@link EnedisPermissionRequestRepository}. Other classes should use
 * {@link EnedisPermissionRequestRepository}.
 */
public interface JpaPermissionRequestRepository extends JpaRepository<EnedisPermissionRequest, String> {
    List<EnedisPermissionRequest> findAllByStatusIs(PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, start_date, end_date, data_need_id, status, granularity, usage_point_id, latest_meter_reading, created FROM fr_enedis.enedis_permission_request WHERE status = 'PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    List<EnedisPermissionRequest> findTimedOutPermissionRequests(@Param("hours") int timeoutDuration);
}

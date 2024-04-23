package energy.eddie.regionconnector.fr.enedis.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FrPermissionRequestRepository extends PermissionRequestRepository<FrEnedisPermissionRequest>, CrudRepository<EnedisPermissionRequest, String> {

    List<FrEnedisPermissionRequest> findAllByStatus(PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, permission_start, permission_end, data_need_id, status, granularity, usage_point_id, latest_meter_reading_end_date, created " +
                    "FROM fr_enedis.enedis_permission_request WHERE status = 'PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    List<EnedisPermissionRequest> findTimedOutPermissionRequests(@Param("hours") int timeoutDuration);
}

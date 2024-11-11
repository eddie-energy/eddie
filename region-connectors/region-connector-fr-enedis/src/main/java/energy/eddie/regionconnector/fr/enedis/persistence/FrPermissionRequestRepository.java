package energy.eddie.regionconnector.fr.enedis.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequestRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FrPermissionRequestRepository extends
        PermissionRequestRepository<FrEnedisPermissionRequest>,
        CrudRepository<EnedisPermissionRequest, String>,
        StalePermissionRequestRepository<EnedisPermissionRequest>,
        CommonPermissionRequestRepository {

    List<FrEnedisPermissionRequest> findByStatus(PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, permission_start, permission_end, data_need_id, status, granularity, usage_point_id, latest_meter_reading_end_date, created, usage_point_type " +
                    "FROM fr_enedis.enedis_permission_request WHERE status = 'VALIDATED' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<EnedisPermissionRequest> findStalePermissionRequests(@Param("hours") int timeoutDuration);
}

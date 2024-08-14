package energy.eddie.regionconnector.fi.fingrid.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.timeout.StalePermissionRequestRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiPermissionRequestRepository extends
        PermissionRequestRepository<FingridPermissionRequest>,
        org.springframework.data.repository.Repository<FingridPermissionRequest, String>,
        StalePermissionRequestRepository<FingridPermissionRequest> {

    @Query(
            value = "SELECT permission_id, connection_id, created, data_need_id, granularity, permission_start, permission_end, status, customer_identification, metering_point, latest_meter_reading " +
                    "FROM fi_fingrid.permission_request WHERE status = 'VALIDATED' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<FingridPermissionRequest> findStalePermissionRequests(@Param("hours") int duration);
}

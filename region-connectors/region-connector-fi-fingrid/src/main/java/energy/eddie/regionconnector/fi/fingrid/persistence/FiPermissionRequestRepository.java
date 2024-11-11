package energy.eddie.regionconnector.fi.fingrid.persistence;

import energy.eddie.api.agnostic.process.model.persistence.FullPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequestRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiPermissionRequestRepository extends
        FullPermissionRequestRepository<FingridPermissionRequest>,
        org.springframework.data.repository.Repository<FingridPermissionRequest, String>,
        CommonPermissionRequestRepository {

    @Query(
            value = "SELECT permission_id, connection_id, created, data_need_id, granularity, permission_start, permission_end, status, customer_identification, metering_point, latest_meter_reading " +
                    "FROM fi_fingrid.permission_request WHERE status = 'VALIDATED' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<FingridPermissionRequest> findStalePermissionRequests(@Param("hours") int duration);

    @Override
    List<FingridPermissionRequest> findByStatus(PermissionProcessStatus status);
}

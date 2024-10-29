package energy.eddie.regionconnector.be.fluvius.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@org.springframework.stereotype.Repository
public interface BePermissionRequestRepository extends
        PermissionRequestRepository<FluviusPermissionRequest>,
        Repository<FluviusPermissionRequest, String>,
        StalePermissionRequestRepository<FluviusPermissionRequest> {
    Iterable<FluviusPermissionRequest> findByStatus(PermissionProcessStatus permissionProcessStatus);

    @Override
    @Query(
            value = "SELECT permission_id, connection_id, data_need_id, status, permission_start, permission_end, granularity, flow, created, short_url_identifier " +
                    "FROM be_fluvius.permission_request WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    List<FluviusPermissionRequest> findStalePermissionRequests(@Param("hours") int stalenessDuration);
}

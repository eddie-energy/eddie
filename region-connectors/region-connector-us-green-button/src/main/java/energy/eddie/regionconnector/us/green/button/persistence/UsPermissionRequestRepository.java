package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.timeout.StalePermissionRequestRepository;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsPermissionRequestRepository extends
        PermissionRequestRepository<UsGreenButtonPermissionRequest>,
        CrudRepository<GreenButtonPermissionRequest, String>,
        StalePermissionRequestRepository<GreenButtonPermissionRequest> {
    List<UsGreenButtonPermissionRequest> findAllByStatus(PermissionProcessStatus status);
    GreenButtonPermissionRequest findByAuthUid(String authUid);

    boolean existsByPermissionIdAndStatus(String permissionId, PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, data_need_id, status, granularity, permission_start, permission_end, dso_id, country_code, jump_off_url, scope, created, auth_uid " +
                    "FROM us_green_button.permission_request WHERE status = 'VALIDATED' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<GreenButtonPermissionRequest> findStalePermissionRequests(@Param("hours") int duration);
}

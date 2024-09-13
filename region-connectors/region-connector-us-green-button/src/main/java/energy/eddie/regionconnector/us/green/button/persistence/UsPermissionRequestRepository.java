package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsPermissionRequestRepository extends PermissionRequestRepository<UsGreenButtonPermissionRequest>, CrudRepository<GreenButtonPermissionRequest, String> {
    List<UsGreenButtonPermissionRequest> findAllByStatus(PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, data_need_id, status, granularity, permission_start, permission_end, dso_id, country_code, jump_off_url, scope, created " +
                    "FROM us_green_button.permission_request WHERE status = 'ACCEPTED' AND polling_status = 'DATA_NOT_READY'",
            nativeQuery = true
    )
    List<GreenButtonPermissionRequest> findAllAcceptedAndNotPolled();
}

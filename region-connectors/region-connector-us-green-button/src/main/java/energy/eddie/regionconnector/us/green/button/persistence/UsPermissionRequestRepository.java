package energy.eddie.regionconnector.us.green.button.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsPermissionRequestRepository extends PermissionRequestRepository<UsGreenButtonPermissionRequest>, CrudRepository<GreenButtonPermissionRequest, String> {
    List<UsGreenButtonPermissionRequest> findAllByStatus(PermissionProcessStatus status);
}

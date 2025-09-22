package energy.eddie.regionconnector.si.moj.elektro.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.si.moj.elektro.permission.request.MojElektroPermissionRequest;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface SiPermissionRequestRepository extends
        Repository<MojElektroPermissionRequest, String>,
        PermissionRequestRepository<MojElektroPermissionRequest> {
    List<MojElektroPermissionRequest> findByStatus(PermissionProcessStatus status);
}

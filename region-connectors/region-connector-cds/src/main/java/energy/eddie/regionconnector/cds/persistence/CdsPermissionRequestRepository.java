package energy.eddie.regionconnector.cds.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CdsPermissionRequestRepository
        extends PermissionRequestRepository<CdsPermissionRequest>,
        org.springframework.data.repository.Repository<CdsPermissionRequest, String> {
    Optional<CdsPermissionRequest> findByState(String state);
}

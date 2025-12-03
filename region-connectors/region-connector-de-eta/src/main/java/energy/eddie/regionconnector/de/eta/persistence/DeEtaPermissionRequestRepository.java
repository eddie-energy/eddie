package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import org.springframework.stereotype.Repository;

@Repository
public interface DeEtaPermissionRequestRepository extends
        PermissionRequestRepository<DeEtaPermissionRequest>,
        StatusPermissionRequestRepository<DeEtaPermissionRequest>,
        org.springframework.data.repository.Repository<DeEtaPermissionRequest, String> {
}

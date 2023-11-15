package energy.eddie.regionconnector.aiida.api;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;

import java.util.Optional;

public interface AiidaPermissionRequestRepository extends PermissionRequestRepository<AiidaPermissionRequest> {
     Optional<AiidaPermissionRequest> findByConnectionId(String connectionId);
}

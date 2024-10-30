package energy.eddie.api.agnostic.process.model.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

public interface FullPermissionRequestRepository<T extends PermissionRequest> extends
        PermissionRequestRepository<T>,
        StalePermissionRequestRepository<T>,
        StatusPermissionRequestRepository<T> {
}

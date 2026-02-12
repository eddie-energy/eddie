package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.process.model.persistence.FullPermissionRequestRepository;

import java.util.Optional;

/**
 * Repository interface for DePermissionRequest.
 * Extends the full permission request repository to provide all necessary operations.
 */
public interface DePermissionRequestRepository extends FullPermissionRequestRepository<DePermissionRequest> {
    
    /**
     * Find a permission request by its permission ID.
     * 
     * @param permissionId the permission ID to search for
     * @return an Optional containing the permission request if found, empty otherwise
     */
    @Override
    Optional<DePermissionRequest> findByPermissionId(String permissionId);
}

package energy.eddie.regionconnector.fr.enedis.permission.request.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This is only a wrapper around the JpaPermissionRequestRepository to make it implement the PermissionRequestRepository interface.
 * It should only be used as a parameter for the {@link energy.eddie.regionconnector.shared.permission.requests.extensions.SavingExtension}
 */
@Component
public class EnedisPermissionRequestRepository implements PermissionRequestRepository<FrEnedisPermissionRequest> {
    private final JpaPermissionRequestRepository repository;

    public EnedisPermissionRequestRepository(JpaPermissionRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(FrEnedisPermissionRequest request) {
        repository.save((EnedisPermissionRequest) request);
    }

    @Override
    public Optional<FrEnedisPermissionRequest> findByPermissionId(String permissionId) {
        return repository.findById(permissionId).map(r -> r);
    }

    /**
     * Deletes the specified permission.
     *
     * @param permissionId the permission id of the request to delete.
     * @return Always true, no matter if the permission was found or not.
     */
    @Override
    public boolean removeByPermissionId(String permissionId) {
        repository.deleteById(permissionId);
        return true;
    }
}

package energy.eddie.regionconnector.fr.enedis.permission.request.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EnedisPermissionRequestRepository implements PermissionRequestRepository<TimeframedPermissionRequest> {
    private final JpaPermissionRequestRepository repository;
    private final StateBuilderFactory factory;

    public EnedisPermissionRequestRepository(JpaPermissionRequestRepository repository, StateBuilderFactory factory) {
        this.repository = repository;
        this.factory = factory;
    }

    @Override
    public void save(TimeframedPermissionRequest request) {
        repository.save((EnedisPermissionRequest) request);
    }

    @Override
    public Optional<TimeframedPermissionRequest> findByPermissionId(String permissionId) {
        return repository.findById(permissionId).map(r -> {
            r.changeState(factory.create(r, r.status()).build());
            return r;
        });
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

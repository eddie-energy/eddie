package energy.eddie.regionconnector.dk.energinet.permission.request.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DkEnerginetCustomerPermissionRequestRepository implements PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> {
    private final JpaPermissionRequestRepository repository;

    public DkEnerginetCustomerPermissionRequestRepository(JpaPermissionRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(DkEnerginetCustomerPermissionRequest request) {
        repository.save((EnerginetCustomerPermissionRequest) request);
    }

    @Override
    public Optional<DkEnerginetCustomerPermissionRequest> findByPermissionId(String permissionId) {
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

    public List<EnerginetCustomerPermissionRequest> findAllByStatusIs(PermissionProcessStatus status) {
        return repository.findAllByStatusIs(status);
    }
}

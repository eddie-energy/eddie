package energy.eddie.regionconnector.es.datadis.permission.request.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Stream;

@Component
public class DatadisPermissionRequestRepository implements EsPermissionRequestRepository {
    private final JpaPermissionRequestRepository repository;

    public DatadisPermissionRequestRepository(JpaPermissionRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(EsPermissionRequest request) {
        repository.save(((DatadisPermissionRequest) request));
    }

    @Override
    public Optional<EsPermissionRequest> findByPermissionId(String permissionId) {
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

    @Override
    public Stream<EsPermissionRequest> findAllAccepted() {
        return repository.findAllByStatusIs(PermissionProcessStatus.ACCEPTED).stream().map(r -> (EsPermissionRequest) r).toList().stream();
    }
}

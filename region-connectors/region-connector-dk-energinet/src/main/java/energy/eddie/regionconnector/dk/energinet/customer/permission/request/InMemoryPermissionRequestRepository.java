package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPermissionRequestRepository implements PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> {
    private final Map<String, DkEnerginetCustomerPermissionRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(DkEnerginetCustomerPermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<DkEnerginetCustomerPermissionRequest> findByPermissionId(String permissionId) {
        if (permissionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }
}

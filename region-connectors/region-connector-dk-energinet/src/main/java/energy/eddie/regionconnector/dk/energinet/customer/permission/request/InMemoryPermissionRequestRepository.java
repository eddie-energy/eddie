package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPermissionRequestRepository implements DkEnerginetCustomerPermissionRequestRepository {
    private final Map<String, DkEnerginetCustomerPermissionRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(DkEnerginetCustomerPermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<DkEnerginetCustomerPermissionRequest> findByPermissionId(String permissionId) {
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }
}

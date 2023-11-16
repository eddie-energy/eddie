package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;

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
        // wrap the request with a SavingPermissionRequest so changes will be persisted
        // TODO this is a temporary workaround
        return Optional.ofNullable(requests.get(permissionId))
                .map(request -> new DkEnerginetCustomerPermissionRequestAdapter(request, new SavingPermissionRequest<>(request, this)));
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }
}

package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPermissionRequestRepository implements PermissionRequestRepository<TimeframedPermissionRequest> {
    private final Map<String, TimeframedPermissionRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(TimeframedPermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<TimeframedPermissionRequest> findByPermissionId(String permissionId) {
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
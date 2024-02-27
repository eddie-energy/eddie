package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPermissionRequestRepository implements PermissionRequestRepository<FrEnedisPermissionRequest> {
    private final Map<String, FrEnedisPermissionRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(FrEnedisPermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<FrEnedisPermissionRequest> findByPermissionId(String permissionId) {
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
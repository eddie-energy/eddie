package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequestRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryAiidaPermissionRequestRepository implements AiidaPermissionRequestRepository {
    private final Map<String, AiidaPermissionRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(AiidaPermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<AiidaPermissionRequest> findByPermissionId(String permissionId) {
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        var removed = requests.remove(permissionId);

        return removed != null;
    }

    @Override
    public Optional<AiidaPermissionRequest> findByConnectionId(String connectionId) {
        return requests.values().stream().filter(entry -> entry.connectionId().equals(connectionId)).findFirst();
    }
}

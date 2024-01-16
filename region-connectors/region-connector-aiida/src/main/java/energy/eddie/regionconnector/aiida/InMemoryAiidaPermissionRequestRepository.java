package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryAiidaPermissionRequestRepository implements AiidaPermissionRequestRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAiidaPermissionRequestRepository.class);
    private final Map<String, AiidaPermissionRequestInterface> requests = new ConcurrentHashMap<>();

    @Override
    public void save(AiidaPermissionRequestInterface request) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Saving request with permission ID {} and status {}", request.permissionId(), request.state().status());

        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<AiidaPermissionRequestInterface> findByPermissionId(String permissionId) {
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        var removed = requests.remove(permissionId);

        return removed != null;
    }
}

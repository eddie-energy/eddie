package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class InMemoryPermissionRequestRepository implements EsPermissionRequestRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryPermissionRequestRepository.class);

    private final Map<String, EsPermissionRequest> requests = new ConcurrentHashMap<>();

    @Override
    public void save(EsPermissionRequest request) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Saving permission request {}", request.permissionId());
        }
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<EsPermissionRequest> findByPermissionId(String permissionId) {
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }

    @Override
    public Stream<EsPermissionRequest> findAllAccepted() {
        return requests.values().stream()
                .filter(request -> request.state().status() == PermissionProcessStatus.ACCEPTED);
    }
}
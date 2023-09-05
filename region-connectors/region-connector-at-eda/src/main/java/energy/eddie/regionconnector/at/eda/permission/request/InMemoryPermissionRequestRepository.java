package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.api.PermissionRequestRepository;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPermissionRequestRepository implements PermissionRequestRepository {

    private final Map<String, PermissionRequest> requests = new ConcurrentHashMap<>();

    private static boolean matchesConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId, PermissionRequest request) {
        return Objects.equals(request.conversationId(), conversationId)
                || Objects.equals(request.cmRequestId(), cmRequestId);
    }

    @Override
    public void save(PermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<PermissionRequest> findByPermissionId(String permissionId) {
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public Optional<PermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId) {
        for (PermissionRequest request : requests.values()) {
            if (matchesConversationIdOrCMRequestId(conversationId, cmRequestId, request)) {
                return Optional.of(request);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }
}

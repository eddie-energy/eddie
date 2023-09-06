package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPermissionRequestRepository implements AtPermissionRequestRepository {

    private final Map<String, AtPermissionRequest> requests = new ConcurrentHashMap<>();

    private static boolean matchesConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId, AtPermissionRequest request) {
        return Objects.equals(request.conversationId(), conversationId)
                || Objects.equals(request.cmRequestId(), cmRequestId);
    }

    @Override
    public void save(AtPermissionRequest request) {
        requests.put(request.permissionId(), request);
    }

    @Override
    public Optional<AtPermissionRequest> findByPermissionId(String permissionId) {
        return Optional.ofNullable(requests.get(permissionId));
    }

    @Override
    public Optional<AtPermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId) {
        for (AtPermissionRequest request : requests.values()) {
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

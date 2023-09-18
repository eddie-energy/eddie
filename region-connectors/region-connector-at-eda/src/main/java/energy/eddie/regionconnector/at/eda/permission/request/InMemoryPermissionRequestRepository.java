package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
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
        // wrap the request with a SavingPermissionRequest so changes will be persisted
        // TODO this is a temporary workaround
        return Optional.ofNullable(requests.get(permissionId))
                .map(r -> new EdaPermissionRequestAdapter(r, new SavingPermissionRequest<>(r, this)));
    }

    @Override
    public Optional<AtPermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId) {
        for (AtPermissionRequest request : requests.values()) {
            if (matchesConversationIdOrCMRequestId(conversationId, cmRequestId, request)) {
                // wrap the request with a SavingPermissionRequest so changes will be persisted
                // TODO this is a temporary workaround
                return Optional.of(request)
                        .map(r -> new EdaPermissionRequestAdapter(r, new SavingPermissionRequest<>(r, this)));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }
}

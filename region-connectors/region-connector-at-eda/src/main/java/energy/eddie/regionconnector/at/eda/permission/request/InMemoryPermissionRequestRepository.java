package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryPermissionRequestRepository implements AtPermissionRequestRepository {

    private final Map<String, AtPermissionRequest> requests = new ConcurrentHashMap<>();

    private static boolean matchesConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId, AtPermissionRequest request) {
        return Objects.equals(request.conversationId(), conversationId)
                || Objects.equals(request.cmRequestId(), cmRequestId);
    }

    private static boolean isInTimeFrame(AtPermissionRequest permissionRequest, LocalDate date) {
        return !date.isBefore(permissionRequest.dataFrom()) && !permissionRequest.dataTo().map(date::isAfter).orElse(false);
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
    @SuppressWarnings("java:S6204")
    // Sonar complains about not using Stream.toList() but, java cant infer the type, so we have to use Collectors.toList()
    public List<AtPermissionRequest> findByMeteringPointIdAndDate(String meteringPointId, LocalDate date) {
        return requests.values().stream()
                .filter(r -> r.meteringPointId().isPresent() && Objects.equals(r.meteringPointId().get(), meteringPointId))
                .filter(r -> isInTimeFrame(r, date))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AtPermissionRequest> findByConsentId(String consentId) {
        return requests.values().stream()
                .filter(r -> r.consentId().map(consentId::equals).orElse(false))
                .findFirst();
    }

    @Override
    public boolean removeByPermissionId(String permissionId) {
        return requests.remove(permissionId) != null;
    }


}

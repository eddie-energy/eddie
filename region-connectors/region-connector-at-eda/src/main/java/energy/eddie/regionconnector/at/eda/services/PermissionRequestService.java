package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PermissionRequestService {
    private final PermissionRequestFactory permissionRequestFactory;
    private final AtPermissionRequestRepository permissionRequestRepository;

    public PermissionRequestService(PermissionRequestFactory permissionRequestFactory, AtPermissionRequestRepository permissionRequestRepository) {
        this.permissionRequestFactory = permissionRequestFactory;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public Optional<AtPermissionRequest> findByPermissionId(String permissionId) {
        return permissionRequestRepository.findByPermissionId(permissionId)
                .map(permissionRequestFactory::create);
    }

    public Optional<AtPermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId) {
        return permissionRequestRepository.findByConversationIdOrCMRequestId(conversationId, cmRequestId)
                .map(permissionRequestFactory::create);
    }

    public List<AtPermissionRequest> findByMeteringPointIdAndDate(String meteringPoint, LocalDate date) {
        return permissionRequestRepository.findByMeteringPointIdAndDate(meteringPoint, date)
                .stream()
                .map(permissionRequestFactory::create)
                .toList();
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return permissionRequestRepository.findByPermissionId(permissionId)
                .map(permissionRequest -> new ConnectionStatusMessage(
                                permissionRequest.connectionId(),
                                permissionRequest.permissionId(),
                                permissionRequest.dataNeedId(),
                                permissionRequest.state().status()
                        )
                );
    }

}

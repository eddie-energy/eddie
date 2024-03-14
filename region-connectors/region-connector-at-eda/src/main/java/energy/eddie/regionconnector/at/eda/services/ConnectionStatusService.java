package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConnectionStatusService {
    private final AtPermissionRequestRepository permissionRequestRepository;

    public ConnectionStatusService(AtPermissionRequestRepository permissionRequestRepository) {
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return permissionRequestRepository.findByPermissionId(permissionId)
                .map(permissionRequest -> new ConnectionStatusMessage(
                                permissionRequest.connectionId(),
                                permissionRequest.permissionId(),
                                permissionRequest.dataNeedId(),
                                permissionRequest.dataSourceInformation(),
                                permissionRequest.status(),
                                permissionRequest.message()
                        )
                );
    }
}
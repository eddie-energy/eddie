package energy.eddie.regionconnector.at.eda.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConnectionStatusService {
    private final AtPermissionRequestRepository permissionRequestRepository;
    private final ObjectMapper objectMapper;

    public ConnectionStatusService(
            AtPermissionRequestRepository permissionRequestRepository,
            ObjectMapper objectMapper
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return permissionRequestRepository.findByPermissionId(permissionId)
                                          .map(permissionRequest -> new ConnectionStatusMessage(
                                                       permissionRequest.connectionId(),
                                                       permissionRequest.permissionId(),
                                                       permissionRequest.dataNeedId(),
                                                       permissionRequest.dataSourceInformation(),
                                                       permissionRequest.status(),
                                                       permissionRequest.message(),
                                                       objectMapper.createObjectNode()
                                                                   .put("cmRequestId", permissionRequest.cmRequestId())
                                               )
                                          );
    }
}

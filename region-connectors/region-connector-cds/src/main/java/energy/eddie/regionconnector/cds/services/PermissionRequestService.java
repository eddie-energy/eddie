package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PermissionRequestService {
    private final CdsPermissionRequestRepository permissionRequestRepository;

    public PermissionRequestService(CdsPermissionRequestRepository permissionRequestRepository) {this.permissionRequestRepository = permissionRequestRepository;}

    public ConnectionStatusMessage getConnectionStatusMessage(String permissionId) throws PermissionNotFoundException {
        return permissionRequestRepository.findByPermissionId(permissionId)
                                          .map(ConnectionStatusMessage::new)
                                          .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }
}

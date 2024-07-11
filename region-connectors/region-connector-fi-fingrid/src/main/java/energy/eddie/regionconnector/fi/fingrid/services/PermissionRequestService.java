package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PermissionRequestService {
    private final FiPermissionRequestRepository permissionRequestRepository;

    public PermissionRequestService(FiPermissionRequestRepository permissionRequestRepository) {this.permissionRequestRepository = permissionRequestRepository;}

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public ConnectionStatusMessage connectionStatusMessage(String permissionId) throws PermissionNotFoundException {
        var permissionRequest = permissionRequestRepository
                .findByPermissionId(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return new ConnectionStatusMessage(
                permissionRequest.connectionId(),
                permissionId,
                permissionRequest.dataNeedId(),
                permissionRequest.dataSourceInformation(),
                permissionRequest.status(),
                null
        );
    }
}

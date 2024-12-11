package energy.eddie.regionconnector.dk.energinet.persistence;

import energy.eddie.api.agnostic.process.model.persistence.FullPermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DkPermissionRequestRepository
        extends PermissionRequestRepository<DkEnerginetPermissionRequest>,
        org.springframework.data.repository.Repository<EnerginetPermissionRequest, String>,
        FullPermissionRequestRepository<DkEnerginetPermissionRequest> {
    List<DkEnerginetPermissionRequest> findAllByStatus(PermissionProcessStatus status);

    RefreshToken getRefreshTokenByPermissionId(String permissionId);

    interface RefreshToken {
        String getRefreshToken();
    }
}

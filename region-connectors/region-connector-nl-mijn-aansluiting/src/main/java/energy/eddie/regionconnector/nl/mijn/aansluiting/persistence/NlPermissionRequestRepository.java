package energy.eddie.regionconnector.nl.mijn.aansluiting.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NlPermissionRequestRepository extends JpaRepository<MijnAansluitingPermissionRequest, String>, PermissionRequestRepository<NlPermissionRequest> {
    Optional<NlPermissionRequest> findByStateAndPermissionId(String state, String permissionId);

    List<NlPermissionRequest> findByStatus(PermissionProcessStatus status);

    boolean existsByPermissionIdAndStatus(String permissionId, PermissionProcessStatus status);
}

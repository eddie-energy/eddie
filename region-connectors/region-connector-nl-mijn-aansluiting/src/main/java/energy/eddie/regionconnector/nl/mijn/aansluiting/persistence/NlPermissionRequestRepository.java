package energy.eddie.regionconnector.nl.mijn.aansluiting.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequestRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NlPermissionRequestRepository extends
        JpaRepository<MijnAansluitingPermissionRequest, String>,
        PermissionRequestRepository<NlPermissionRequest>,
        StalePermissionRequestRepository<MijnAansluitingPermissionRequest>,
        CommonPermissionRequestRepository {
    Optional<NlPermissionRequest> findByStateAndPermissionId(String state, String permissionId);

    List<NlPermissionRequest> findByStatus(PermissionProcessStatus status);

    boolean existsByPermissionIdAndStatus(String permissionId, PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, created, data_need_id, granularity, permission_start, permission_end, status, state, code_verifier " +
                    "FROM nl_mijn_aansluiting.permission_request WHERE status = 'VALIDATED' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<MijnAansluitingPermissionRequest> findStalePermissionRequests(@Param("hours") int timeoutDuration);
}

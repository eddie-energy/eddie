// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface NlPermissionRequestRepository extends
        Repository<MijnAansluitingPermissionRequest, String>,
        PermissionRequestRepository<MijnAansluitingPermissionRequest>,
        StalePermissionRequestRepository<MijnAansluitingPermissionRequest>,
        StatusPermissionRequestRepository<MijnAansluitingPermissionRequest> {
    Optional<MijnAansluitingPermissionRequest> findByStateAndPermissionId(String state, String permissionId);

    @Override
    List<MijnAansluitingPermissionRequest> findByStatus(PermissionProcessStatus status);

    boolean existsByPermissionIdAndStatus(String permissionId, PermissionProcessStatus status);

    @Query(
            value = "SELECT * " +
                    "FROM nl_mijn_aansluiting.permission_request WHERE status = 'VALIDATED' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<MijnAansluitingPermissionRequest> findStalePermissionRequests(@Param("hours") int timeoutDuration);
}

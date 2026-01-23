// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DkPermissionRequestRepository
        extends PermissionRequestRepository<DkEnerginetPermissionRequest>,
        org.springframework.data.repository.Repository<EnerginetPermissionRequest, String>,
        StatusPermissionRequestRepository<DkEnerginetPermissionRequest> {
    List<DkEnerginetPermissionRequest> findAllByStatus(PermissionProcessStatus status);

}

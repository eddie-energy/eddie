// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.regionconnector.si.moj.elektro.permission.request.MojElektroPermissionRequest;
import org.springframework.stereotype.Repository;

@Repository
public interface SiPermissionRequestRepository extends
        org.springframework.data.repository.Repository<MojElektroPermissionRequest, String>,
        PermissionRequestRepository<MojElektroPermissionRequest> {
}

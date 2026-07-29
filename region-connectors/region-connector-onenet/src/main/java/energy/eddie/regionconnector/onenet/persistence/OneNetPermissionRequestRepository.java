// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.regionconnector.onenet.permission.request.OneNetPermissionRequest;
import org.springframework.stereotype.Repository;

@Repository
public interface OneNetPermissionRequestRepository
        extends PermissionRequestRepository<OneNetPermissionRequest>,
        org.springframework.data.repository.Repository<OneNetPermissionRequest, String> {
}

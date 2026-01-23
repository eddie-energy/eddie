// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.process.model.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

public interface FullPermissionRequestRepository<T extends PermissionRequest> extends
        PermissionRequestRepository<T>,
        StalePermissionRequestRepository<T>,
        StatusPermissionRequestRepository<T> {
}

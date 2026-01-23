// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.process.model.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.util.List;

@FunctionalInterface
public interface StatusPermissionRequestRepository<T extends PermissionRequest> {
    List<T> findByStatus(PermissionProcessStatus status);
}

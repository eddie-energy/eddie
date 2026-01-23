// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;

import java.util.Optional;

public interface PermissionEventRepositories {
    Optional<PermissionEventRepository> getPermissionEventRepositoryByRegionConnectorId(String regionConnectorId);
}

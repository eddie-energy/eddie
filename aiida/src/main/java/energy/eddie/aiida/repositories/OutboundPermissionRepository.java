// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Queries only {@link OutboundPermission}s, see {@link PermissionRepository} for queries across both directions.
 */
public interface OutboundPermissionRepository extends JpaRepository<OutboundPermission, UUID> {
    List<OutboundPermission> findByDataSourceIdAndStatus(UUID dataSourceId, PermissionStatus status);
}

// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Queries only {@link InboundPermission}s, see {@link PermissionRepository} for queries across both directions.
 */
public interface InboundPermissionRepository extends JpaRepository<InboundPermission, UUID> {
    Optional<InboundPermission> findByPermissionIdAndUserId(UUID permissionId, UUID userId);

    boolean existsByPermissionIdAndUserId(UUID permissionId, UUID userId);

    Optional<InboundPermission> findByDataSourceId(UUID dataSourceId);

    List<InboundPermission> findByUserIdAndDataSourceIsNotNullAndStatusOrderByGrantTimeDesc(
            UUID userId,
            PermissionStatus status
    );

    List<InboundPermission> findByMonitoringDataSourceIdIsNotNullAndStatus(PermissionStatus status);
}

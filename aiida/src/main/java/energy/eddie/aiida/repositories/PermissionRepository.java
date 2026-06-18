// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByUserIdOrderByGrantTimeDesc(UUID userId);

    List<Permission> findByStatusIn(Set<PermissionStatus> statuses);

    @Query("""
            SELECT p.permissionId
            FROM Permission p
            WHERE p.dataSource.id = :dataSourceId
              AND p.dataNeed.type = energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed.DISCRIMINATOR_VALUE
              AND p.status IN (:statuses)
            """)
    List<UUID> findOutboundByDataSourceIdAndStatus(UUID dataSourceId, Set<PermissionStatus> statuses);

    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.userId = :userId
              AND p.dataNeed.type = energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed.DISCRIMINATOR_VALUE
              AND p.dataSource IS NOT NULL
              AND p.status IN (:statuses)
            ORDER BY p.grantTime DESC
            """)
    List<Permission> findInboundByUserIdAndStatus(UUID userId, Set<PermissionStatus> statuses);
}

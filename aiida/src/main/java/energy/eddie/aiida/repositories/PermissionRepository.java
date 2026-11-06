// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByUserIdOrderByGrantTimeDescRevokeTimeDesc(UUID userId);

    List<Permission> findByMonitoringDataSourceIdIsNotNullAndStatus(PermissionStatus status);

    /**
     * Returns all permissions with the given status that have received their MQTT credentials.
     */
    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.status = :status
              AND p.mqttStreamingConfig IS NOT NULL
            """)
    List<Permission> findStartableByStatus(PermissionStatus status);

    Optional<Permission> findByPermissionIdAndUserId(UUID permissionId, UUID userId);

    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.dataSource.id = :dataSourceId
              AND p.dataNeed.type = energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed.DISCRIMINATOR_VALUE
              AND p.status = :status
            """)
    List<Permission> findOutboundByDataSourceIdAndStatus(UUID dataSourceId, PermissionStatus status);

    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.dataSource.id = :dataSourceId
              AND p.dataNeed.type = energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed.DISCRIMINATOR_VALUE
            """)
    Optional<Permission> findInboundByDataSourceId(UUID dataSourceId);

    @Query("""
            SELECT p
            FROM Permission p
            WHERE p.userId = :userId
              AND p.dataNeed.type = energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed.DISCRIMINATOR_VALUE
              AND p.dataSource IS NOT NULL
              AND p.status = :status
            ORDER BY p.grantTime DESC
            """)
    List<Permission> findInboundByUserIdAndStatus(UUID userId, PermissionStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM Permission p
            JOIN p.dataNeed dn
            JOIN dn.contexts c
            WHERE p.userId = :userId
              AND p.meterId = :meterId
              AND p.status = :status
              AND dn.type = :dataNeedType
              AND c = :context
            """)
    boolean existsByUserIdAndDataNeedTypeAndContextAndMeterIdAndStatus(
            UUID userId,
            String dataNeedType,
            AiidaContext context,
            String meterId,
            PermissionStatus status
    );

}

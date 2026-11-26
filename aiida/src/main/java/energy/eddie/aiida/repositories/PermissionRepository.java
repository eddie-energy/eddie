// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Queries permissions of both directions, see {@link InboundPermissionRepository} and
 * {@link OutboundPermissionRepository} for direction specific queries.
 */
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByUserIdOrderByGrantTimeDescRevokeTimeDesc(UUID userId);

    List<Permission> findByMqttStreamingConfigIsNotNullAndStatus(PermissionStatus status);

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

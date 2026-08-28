// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ConnectionLimitDefaultRepository extends JpaRepository<ConnectionLimitDefault, UUID> {
    @Query("""
            SELECT d, p.meterId
            FROM ConnectionLimitDefault d, Permission p
            WHERE p.userId = :userId
              AND p.permissionId = d.permissionId
              AND (:permissionId IS NULL OR d.permissionId = :permissionId)
            """)
    List<ConnectionLimitDefault> findByUserIdAndPermissionId(UUID userId, @Nullable UUID permissionId);
}

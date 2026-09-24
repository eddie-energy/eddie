// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConnectionLimitDefaultRepository extends JpaRepository<ConnectionLimitDefault, Long> {

    @Query("""
            SELECT d
            FROM ConnectionLimitDefault d, Permission p
            WHERE d.permissionId = p.permissionId
              AND p.userId = :userId
              AND (:permissionId IS NULL OR p.permissionId = :permissionId)
              AND d.start < :to AND (d.end IS NULL OR d.end > :from)
            """)
    List<ConnectionLimitDefault> findByUserIdAndPermissionId(
            UUID userId,
            @Nullable UUID permissionId,
            Instant from,
            Instant to
    );

    /**
     * Ends all open defaults of a permission at the given instant, keeping them as historical defaults.
     */
    @Modifying
    @Query("""
            UPDATE ConnectionLimitDefault d SET d.end = :end
            WHERE d.permissionId = :permissionId AND d.end IS NULL
            """)
    void closeOpenDefaults(UUID permissionId, Instant end);
}
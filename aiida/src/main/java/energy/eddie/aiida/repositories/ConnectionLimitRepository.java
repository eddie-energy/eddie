// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionLimitRepository extends JpaRepository<ConnectionLimit, ConnectionLimit.ConnectionLimitKey> {
    @Query("""
            SELECT cl
            FROM ConnectionLimit cl, Permission p
            WHERE p.permissionId = cl.permissionId
              AND p.userId = :userId
              AND (:permissionId IS NULL OR cl.permissionId = :permissionId)
              AND (:meterId IS NULL OR cl.meterId = :meterId)
              AND cl.intervalEnd > :from
              AND cl.intervalStart <= :to
            ORDER BY
              cl.permissionId,
              cl.meterId,
              cl.intervalStart,
              cl.intervalEnd
            """)
    List<ConnectionLimit> findByUserIdAndFiltersFromTo(
            UUID userId,
            @Nullable UUID permissionId,
            @Nullable String meterId,
            Instant from,
            Instant to
    );

    @Query("SELECT MAX(cl.revisionNumber) FROM ConnectionLimit cl WHERE cl.mrid = :mrid")
    Optional<Integer> findMaxRevisionNumberByMrid(@Param("mrid") String mrid);

    void deleteByMrid(String mrid);

    @Query("""
            SELECT cl.createdAt
            FROM ConnectionLimit cl
            WHERE cl.permissionId = :permissionId
              AND cl.meterId = :meterId
              AND cl.intervalStart = :intervalStart
              AND cl.intervalEnd = :intervalEnd
            """)
    Optional<Instant> findCreatedAtByPermissionMeterAndInterval(
            @Param("permissionId") UUID permissionId,
            @Param("meterId") String meterId,
            @Param("intervalStart") Instant intervalStart,
            @Param("intervalEnd") Instant intervalEnd
    );
}

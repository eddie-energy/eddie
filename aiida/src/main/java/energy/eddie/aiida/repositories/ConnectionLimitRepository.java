// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConnectionLimitRepository extends JpaRepository<ConnectionLimit, Long> {
    @Query("""
            SELECT cl
            FROM ConnectionLimit cl
            WHERE cl.permissionId = :permissionId
              AND cl.intervalEnd > :from
              AND cl.intervalStart <= COALESCE(:to, cl.intervalStart)
            ORDER BY cl.intervalStart
            """)
    List<ConnectionLimit> findByPermissionIdFromTo(
            UUID permissionId,
            Instant from,
            @Nullable Instant to,
            Pageable pageable
    );
}

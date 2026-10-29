// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConnectionLimitViolationRepository extends JpaRepository<ConnectionLimitViolation, Long> {
    List<ConnectionLimitViolation> findByEndedAtIsNull();

    @Query("""
            SELECT v
            FROM ConnectionLimitViolation v
            WHERE v.permissionId = :permissionId
              AND v.startedAt <= :to
              AND (v.endedAt IS NULL OR v.endedAt >= :from)
            ORDER BY v.startedAt
            """)
    List<ConnectionLimitViolation> findOverlapping(
            @Param("permissionId") UUID permissionId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}

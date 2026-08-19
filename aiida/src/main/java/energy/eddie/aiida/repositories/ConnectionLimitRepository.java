// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionLimitRepository extends JpaRepository<ConnectionLimit, ConnectionLimit.ConnectionLimitKey> {
    @Query("""
            SELECT cl
            FROM ConnectionLimit cl, Permission p
            WHERE cl.permissionId = p.permissionId
              AND p.userId = :userId
              AND cl.permissionId = COALESCE(:permissionId, cl.permissionId)
              AND COALESCE(cl.meterId, '') = COALESCE(:meterId, COALESCE(cl.meterId, ''))
              AND cl.intervalEnd > :from
              AND cl.intervalStart <= COALESCE(:to, cl.intervalStart)
            ORDER BY cl.intervalStart
            """)
    List<ConnectionLimit> findByUserIdAndFiltersFromTo(
            UUID userId,
            @Nullable UUID permissionId,
            @Nullable String meterId,
            Instant from,
            @Nullable Instant to,
            Pageable pageable
    );

    @Query(value = """
            WITH
            request AS (
                SELECT tstzrange(:from, CAST(:to AS timestamptz), '[)') AS req_range
            ),
            candidates AS (
                SELECT cl.permission_id,
                       cl.meter_id,
                       tstzrange(cl.interval_start, cl.interval_end, '[)') * r.req_range AS clipped,
                       cl.min_limit_kw,
                       cl.max_limit_kw,
                       cl.created_at,
                       cl.revision_number,
                       cl.mrid
                FROM connection_limit cl
                JOIN permission p ON cl.permission_id = p.permission_id
                CROSS JOIN request r
                WHERE p.user_id = :userId
                  AND (:permissionId IS NULL OR cl.permission_id = :permissionId)
                  AND (:meterId IS NULL OR cl.meter_id = :meterId)
                  AND tstzrange(cl.interval_start, cl.interval_end, '[)') && r.req_range
            ),
            boundaries AS (
                SELECT lower(clipped) AS b FROM candidates
                UNION
                SELECT upper(clipped) FROM candidates
                UNION
                SELECT lower(req_range) FROM request
                UNION
                SELECT upper(req_range) FROM request WHERE NOT upper_inf(req_range)
            ),
            segments AS (
                SELECT b AS segment_start,
                       LEAD(b) OVER (ORDER BY b) AS segment_end
                FROM boundaries
            ),
            ranked AS (
                SELECT COALESCE(c.permission_id, CAST(:permissionId AS uuid)) AS permission_id,
                       COALESCE(c.meter_id, :meterId)                         AS meter_id,
                       s.segment_start AS interval_start,
                       s.segment_end   AS interval_end,
                       COALESCE(c.min_limit_kw, CAST(:defaultMinLimitKw AS numeric)) AS min_limit_kw,
                       COALESCE(c.max_limit_kw, CAST(:defaultMaxLimitKw AS numeric)) AS max_limit_kw,
                       c.mrid,
                       ROW_NUMBER() OVER (
                           PARTITION BY c.permission_id, c.meter_id, s.segment_start, s.segment_end
                           ORDER BY c.created_at DESC, c.revision_number DESC
                       ) AS rn
                FROM segments s
                LEFT JOIN candidates c ON c.clipped && tstzrange(s.segment_start, s.segment_end, '[)')
                WHERE s.segment_end > s.segment_start
                  AND (c.permission_id IS NOT NULL OR CAST(:defaultMinLimitKw AS numeric) IS NOT NULL)
            ),
            with_groups AS (
                SELECT permission_id, meter_id, interval_start, interval_end,
                       min_limit_kw, max_limit_kw, mrid,
                       CASE
                           WHEN LAG(interval_end)  OVER w = interval_start
                            AND LAG(min_limit_kw)  OVER w = min_limit_kw
                            AND LAG(max_limit_kw)  OVER w = max_limit_kw
                            AND LAG(mrid)          OVER w IS NOT DISTINCT FROM mrid
                           THEN 0
                           ELSE 1
                       END AS is_new_group
                FROM ranked
                WHERE rn = 1
                WINDOW w AS (
                    PARTITION BY permission_id, meter_id
                    ORDER BY interval_start, interval_end
                )
            ),
            grouped AS (
                SELECT permission_id, meter_id, interval_start, interval_end,
                       min_limit_kw, max_limit_kw, mrid,
                       SUM(is_new_group) OVER (
                           PARTITION BY permission_id, meter_id
                           ORDER BY interval_start, interval_end
                           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                       ) AS grp
                FROM with_groups
            )
            SELECT permission_id AS permissionid,
                   meter_id      AS meterid,
                   MIN(interval_start) AS intervalstart,
                   MAX(interval_end)   AS intervalend,
                   min_limit_kw AS minlimitkw,
                   max_limit_kw AS maxlimitkw,
                   mrid         AS mrid
            FROM grouped
            GROUP BY permission_id, meter_id, grp, min_limit_kw, max_limit_kw, mrid
            ORDER BY intervalstart, permissionid, meterid
            LIMIT CAST(:limit AS INTEGER)
            """, nativeQuery = true)
    List<EffectiveConnectionLimitProjection> findEffectiveByUserIdAndFiltersFromTo(
            @Param("userId") UUID userId,
            @Param("permissionId") @Nullable UUID permissionId,
            @Param("meterId") @Nullable String meterId,
            @Param("from") Instant from,
            @Param("to") @Nullable Instant to,
            @Param("limit") @Nullable Integer limit,
            @Param("defaultMinLimitKw") @Nullable BigDecimal defaultMinLimitKw,
            @Param("defaultMaxLimitKw") @Nullable BigDecimal defaultMaxLimitKw
    );

    default List<EffectiveConnectionLimitProjection> findEffectiveByUserIdAndFiltersFromTo(
            UUID userId,
            @Nullable UUID permissionId,
            @Nullable String meterId,
            Instant from,
            @Nullable Instant to,
            @Nullable Integer limit
    ) {
        return findEffectiveByUserIdAndFiltersFromTo(userId, permissionId, meterId, from, to, limit, null, null);
    }

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

    interface EffectiveConnectionLimitProjection {
        UUID getPermissionId();

        String getMeterId();

        Instant getIntervalStart();

        Instant getIntervalEnd();

        BigDecimal getMinLimitKw();

        BigDecimal getMaxLimitKw();

        @Nullable
        String getMrid();
    }
}

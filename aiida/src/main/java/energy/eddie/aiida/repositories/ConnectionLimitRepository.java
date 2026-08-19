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
                SELECT TSTZRANGE(:from, :to, '[)') AS req_range
            ),

            -- Permissions visible to the user.
            permissions AS (
                SELECT p.permission_id
                FROM permission p
                WHERE p.user_id = :userId
                  AND (:permissionId IS NULL OR p.permission_id = :permissionId)
            ),

            -- Per-permission defaults if configured. Either default value may independently be NULL.
            permission_defaults AS (
                SELECT
                    p.permission_id,
                    d.default_min_limit_kw,
                    d.default_max_limit_kw,
                    d.permission_id IS NOT NULL AS has_default
                FROM permissions p
                LEFT JOIN permission_connection_limit_defaults d
                  ON d.permission_id = p.permission_id
            ),

            -- Determine the meter scopes for which to calculate the effective timeline.
            -- With a specific meter the request produces exactly that meter.
            -- Without a meter it calculates one timeline per meter.
            -- The empty string is the special "no specific meter" scope.
            scopes AS (
                SELECT
                    p.permission_id,
                    :meterId AS meter_id
                FROM permissions p
                WHERE :meterId IS NOT NULL

                UNION

                SELECT
                    cl.permission_id,
                    cl.meter_id
                FROM connection_limit cl
                JOIN permissions p
                  ON p.permission_id = cl.permission_id

                UNION

                SELECT
                    p.permission_id,
                    '' AS meter_id
                FROM permissions p
                WHERE :meterId IS NULL
            ),

            -- Candidate limits for each scope.
            candidates AS (
                SELECT
                    cl.permission_id,
                    cl.meter_id,
                    cl.time_frame * r.req_range AS clipped,
                    cl.min_limit_kw,
                    cl.max_limit_kw,
                    cl.created_at,
                    cl.revision_number,
                    cl.mrid
                FROM connection_limit cl
                JOIN scopes s
                  ON s.permission_id = cl.permission_id
                 AND s.meter_id = cl.meter_id
                CROSS JOIN request r
                WHERE cl.time_frame && r.req_range
            ),

            -- Boundaries per permission or meter to ensure no result is outside [from,to).
            boundaries AS (
                SELECT
                    s.permission_id,
                    s.meter_id,
                    LOWER(r.req_range) AS boundary
                FROM scopes s
                CROSS JOIN request r
            
                UNION

                SELECT
                    s.permission_id,
                    s.meter_id,
                    UPPER(r.req_range) AS boundary
                FROM scopes s
                CROSS JOIN request r
            
                UNION

                SELECT
                    c.permission_id,
                    c.meter_id,
                    LOWER(c.clipped) AS boundary
                FROM candidates c
            
                UNION

                SELECT
                    c.permission_id,
                    c.meter_id,
                    UPPER(c.clipped) AS boundary
                FROM candidates c
            ),

            -- Convert consecutive boundaries into atomic segments for which candidates cannot change.
            segments AS (
                SELECT
                    permission_id,
                    meter_id,
                    boundary AS segment_start,
                    LEAD(boundary) OVER (
                        PARTITION BY permission_id, meter_id
                        ORDER BY boundary
                    ) AS segment_end
                FROM boundaries
            ),
            
            -- Resolve newest connection limit or default per segment.
            resolved AS (
                SELECT
                    s.permission_id,
                    s.meter_id,
                    s.segment_start AS interval_start,
                    s.segment_end   AS interval_end,
            
                    CASE
                        WHEN c.permission_id IS NOT NULL
                            THEN c.min_limit_kw
                        WHEN d.has_default
                            THEN d.default_min_limit_kw
                        ELSE NULL
                    END AS min_limit_kw,
            
                    CASE
                        WHEN c.permission_id IS NOT NULL
                            THEN c.max_limit_kw
                        WHEN d.has_default
                            THEN d.default_max_limit_kw
                        ELSE NULL
                    END AS max_limit_kw,
            
                    c.mrid,

                    -- Ignore segments with neither connection limit nor default
                    c.permission_id IS NOT NULL AS has_connection_limit,
                    d.has_default
                FROM segments s
            
                LEFT JOIN LATERAL (
                    SELECT c.*
                    FROM candidates c
                    WHERE c.permission_id = s.permission_id
                      AND c.meter_id = s.meter_id
                      AND c.clipped @> s.segment_start
                    ORDER BY
                        c.created_at DESC,
                        c.revision_number DESC,
                        c.mrid DESC
                    LIMIT 1
                ) c ON TRUE

                JOIN permission_defaults d
                  ON d.permission_id = s.permission_id

                WHERE s.segment_end > s.segment_start
                  AND (c.permission_id IS NOT NULL OR d.has_default)
            ),

            -- Mark adjacent segments with identical effective values so they can be merged back into larger intervals.
            -- IS NOT DISTINCT FROM is intentional because min/max defaults are allowed to be NULL.
            with_groups AS (
                SELECT
                    permission_id,
                    meter_id,
                    interval_start,
                    interval_end,
                    min_limit_kw,
                    max_limit_kw,
                    mrid,

                    CASE
                        WHEN LAG(interval_end) OVER w = interval_start
                         AND LAG(min_limit_kw) OVER w IS NOT DISTINCT FROM min_limit_kw
                         AND LAG(max_limit_kw) OVER w IS NOT DISTINCT FROM max_limit_kw
                         AND LAG(mrid) OVER w IS NOT DISTINCT FROM mrid
                        THEN 0
                        ELSE 1
                    END AS is_new_group
                FROM resolved
            
                WINDOW w AS (
                    PARTITION BY permission_id, meter_id
                    ORDER BY interval_start, interval_end
                )
            ),

            -- Assign a group number to each run of identical effective values.
            grouped AS (
                SELECT
                    permission_id,
                    meter_id,
                    interval_start,
                    interval_end,
                    min_limit_kw,
                    max_limit_kw,
                    mrid,
            
                    SUM(is_new_group) OVER (
                        PARTITION BY permission_id, meter_id
                        ORDER BY interval_start, interval_end
                        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                    ) AS grp
                FROM with_groups
            )

            -- Merge each run back into a single interval.
            SELECT
                permission_id AS permissionid,
                meter_id      AS meterid,
                MIN(interval_start) AS intervalstart,
                MAX(interval_end)   AS intervalend,
                min_limit_kw AS minlimitkw,
                max_limit_kw AS maxlimitkw,
                mrid
            FROM grouped
            GROUP BY
                permission_id,
                meter_id,
                grp,
                min_limit_kw,
                max_limit_kw,
                mrid
            ORDER BY
                intervalstart,
                permission_id,
                meter_id
            """, nativeQuery = true)
    List<EffectiveConnectionLimitProjection> findEffectiveByUserIdAndFiltersFromTo(
            @Param("userId") UUID userId,
            @Param("permissionId") @Nullable UUID permissionId,
            @Param("meterId") @Nullable String meterId,
            @Param("from") Instant from,
            @Param("to") Instant to
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

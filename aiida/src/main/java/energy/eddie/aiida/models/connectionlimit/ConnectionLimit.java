// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(ConnectionLimit.ConnectionLimitKey.class)
@Table(name = "connection_limit")
public class ConnectionLimit {

    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    /**
     * A missing meter id is persisted as an empty string so the meter id can be a primary key.
     */
    @Id
    @Column(name = "meter_id", nullable = false, updatable = false)
    private String meterId;

    @Id
    @Column(name = "interval_start", nullable = false, updatable = false)
    private Instant intervalStart;

    @Id
    @Column(name = "interval_end", nullable = false)
    private Instant intervalEnd;

    @Column(name = "min_limit_kw", nullable = false)
    private BigDecimal minLimitKw;

    @Column(name = "max_limit_kw", nullable = false)
    private BigDecimal maxLimitKw;

    @Column(name = "mrid", nullable = false)
    private String mrid;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @SuppressWarnings("NullAway.Init")
    protected ConnectionLimit() {
    }

    public ConnectionLimit(
            UUID permissionId,
            @Nullable String meterId,
            Instant intervalStart,
            Instant intervalEnd,
            BigDecimal minLimitKw,
            BigDecimal maxLimitKw,
            String mrid,
            int revisionNumber,
            Instant createdAt
    ) {
        this.permissionId = permissionId;
        this.meterId = meterId == null ? "" : meterId;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
        this.mrid = mrid;
        this.revisionNumber = revisionNumber;
        this.createdAt = createdAt;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public String meterId() {
        return meterId;
    }

    public Instant intervalStart() {
        return intervalStart;
    }

    public Instant intervalEnd() {
        return intervalEnd;
    }

    public BigDecimal minLimitKw() {
        return minLimitKw;
    }

    public BigDecimal maxLimitKw() {
        return maxLimitKw;
    }

    public String mrid() {
        return mrid;
    }

    public int revisionNumber() {
        return revisionNumber;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public record ConnectionLimitKey(UUID permissionId, String meterId, Instant intervalStart, Instant intervalEnd) {}
}

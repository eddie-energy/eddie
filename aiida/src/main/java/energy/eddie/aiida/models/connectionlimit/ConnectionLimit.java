// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connection_limit",
       uniqueConstraints = @UniqueConstraint(columnNames = {"permission_id", "interval_start"}))
public class ConnectionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings({"unused", "NullAway"})
    private Long id;

    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    @Nullable
    @Column(name = "meter_id")
    private String meterId;

    @Column(name = "interval_start", nullable = false, updatable = false)
    private Instant intervalStart;

    @Column(name = "interval_end", nullable = false)
    private Instant intervalEnd;

    @Column(name = "min_limit_kw", nullable = false)
    private BigDecimal minLimitKw;

    @Column(name = "max_limit_kw", nullable = false)
    private BigDecimal maxLimitKw;

    @SuppressWarnings("NullAway.Init")
    protected ConnectionLimit() {
    }

    public ConnectionLimit(
            UUID permissionId,
            @Nullable String meterId,
            Instant intervalStart,
            Instant intervalEnd,
            BigDecimal minLimitKw,
            BigDecimal maxLimitKw
    ) {
        this.permissionId = permissionId;
        this.meterId = meterId;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public @Nullable String meterId() {
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
}

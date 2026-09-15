// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A default connection limit of a permission, forming an append-only timeline scoped to the permission.
 * {@link #start()} records when the eligible party set the default and {@link #end()} when a newer default replaced it.
 * The current default has {@link #end()} equal {@code null}.
 */
@Entity
@SuppressWarnings("NullAway.Init")
@Table(name = "connection_limit_default")
public class ConnectionLimitDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    @Column(name = "meter_id", nullable = false, updatable = false)
    private String meterId;

    @Column(name = "start", nullable = false, updatable = false)
    private Instant start;

    @Column(name = "replaced_at", updatable = false)
    @Nullable
    private Instant end;

    @Column(name = "min_limit_kw")
    @Nullable
    private BigDecimal minLimitKw;

    @Column(name = "max_limit_kw")
    @Nullable
    private BigDecimal maxLimitKw;

    protected ConnectionLimitDefault() {}

    public ConnectionLimitDefault(
            UUID permissionId,
            @Nullable String meterId,
            Instant start,
            @Nullable Instant end,
            @Nullable BigDecimal minLimitKw,
            @Nullable BigDecimal maxLimitKw
    ) {
        this.permissionId = permissionId;
        this.meterId = meterId == null ? "" : meterId;
        this.start = start;
        this.end = end;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
    }

    public Long id() {
        return id;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public String meterId() {
        return meterId;
    }

    public Instant start() {
        return start;
    }

    public @Nullable Instant end() {
        return end;
    }

    public @Nullable BigDecimal minLimitKw() {
        return minLimitKw;
    }

    public @Nullable BigDecimal maxLimitKw() {
        return maxLimitKw;
    }
}

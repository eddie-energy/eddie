// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "connection_limit_default")
public class ConnectionLimitDefault {

    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    @Transient
    @Nullable
    private String meterId;

    @Column(name = "default_min_limit_kw", nullable = false)
    private BigDecimal minLimitKw;

    @Column(name = "default_max_limit_kw", nullable = false)
    private BigDecimal maxLimitKw;

    @SuppressWarnings("NullAway.Init")
    protected ConnectionLimitDefault() {}

    public ConnectionLimitDefault(
            UUID permissionId,
            @Nullable String meterId,
            BigDecimal minLimitKw,
            BigDecimal maxLimitKw
    ) {
        this.permissionId = permissionId;
        this.meterId = meterId;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public @Nullable String meterId() {
        return meterId;
    }

    public BigDecimal minLimitKw() {
        return minLimitKw;
    }

    public BigDecimal maxLimitKw() {
        return maxLimitKw;
    }
}

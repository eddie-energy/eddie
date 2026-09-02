// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Entity(name = "AiidaCreatedEvent")
public class CreatedEvent extends PersistablePermissionEvent {
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Nullable
    @Column(name = "meter_id")
    private final String meterId;
    @Nullable
    @Column(name = "min_limit_kw")
    private final BigDecimal minLimitKw;
    @Nullable
    @Column(name = "max_limit_kw")
    private final BigDecimal maxLimitKw;
    @Column(name = "permission_start")
    private final LocalDate permissionStart;
    @Column(name = "permission_end")
    private final LocalDate permissionEnd;

    @SuppressWarnings("NullAway") // Needed for JPA
    protected CreatedEvent() {
        this.connectionId = null;
        this.dataNeedId = null;
        this.meterId = null;
        this.minLimitKw = null;
        this.maxLimitKw = null;
        this.permissionStart = null;
        this.permissionEnd = null;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            @Nullable String meterId,
            @Nullable BigDecimal minLimitKw,
            @Nullable BigDecimal maxLimitKw,
            LocalDate permissionStart,
            LocalDate permissionEnd,
            Clock clock
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, clock);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.meterId = meterId;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            @Nullable String meterId,
            @Nullable BigDecimal minLimitKw,
            @Nullable BigDecimal maxLimitKw,
            LocalDate permissionStart,
            LocalDate permissionEnd
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.meterId = meterId;
        this.minLimitKw = minLimitKw;
        this.maxLimitKw = maxLimitKw;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    @Nullable
    public String meterId() {
        return meterId;
    }

    @Nullable
    public BigDecimal minLimitKw() {
        return minLimitKw;
    }

    @Nullable
    public BigDecimal maxLimitKw() {
        return maxLimitKw;
    }

    public LocalDate permissionStart() {
        return permissionStart;
    }

    public LocalDate permissionEnd() {
        return permissionEnd;
    }
}

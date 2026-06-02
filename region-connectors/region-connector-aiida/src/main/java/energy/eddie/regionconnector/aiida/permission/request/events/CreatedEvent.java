// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.Clock;
import java.time.LocalDate;

@Entity(name = "AiidaCreatedEvent")
public class CreatedEvent extends PersistablePermissionEvent {
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "permission_start")
    private final LocalDate permissionStart;
    @Column(name = "permission_end")
    private final LocalDate permissionEnd;

    @SuppressWarnings("NullAway") // Needed for JPA
    protected CreatedEvent() {
        this.connectionId = null;
        this.dataNeedId = null;
        this.permissionStart = null;
        this.permissionEnd = null;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate permissionStart,
            LocalDate permissionEnd,
            Clock clock
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, clock);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate permissionStart,
            LocalDate permissionEnd
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public LocalDate permissionStart() {
        return permissionStart;
    }

    public LocalDate permissionEnd() {
        return permissionEnd;
    }
}

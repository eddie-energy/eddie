// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "DatadisPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "es_datadis", name = "permission_event")
@SuppressWarnings({"NullAway", "unused"})
public abstract class PersistablePermissionEvent implements PermissionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    // Aggregate ID
    @Column(length = 36)
    private final String permissionId;
    private final ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;

    protected PersistablePermissionEvent(
            String permissionId,
            PermissionProcessStatus status
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
        this.status = status;
    }

    protected PersistablePermissionEvent() {
        this.id = null;
        permissionId = null;
        eventCreated = null;
        status = null;
    }

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status, Clock clock) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(clock);
        this.status = status;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public ZonedDateTime eventCreated() {
        return eventCreated;
    }
}
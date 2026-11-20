// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering.permission.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "EleringPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "ee_elering", name = "permission_event")
@SuppressWarnings("NullAway")
public abstract class PersistablePermissionEvent implements PermissionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("unused")
    private Long id;
    @Column(length = 36)
    private String permissionId;
    private ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private PermissionProcessStatus status;

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status) {
        this.permissionId = permissionId;
        this.status = status;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
    }

    protected PersistablePermissionEvent() { }

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

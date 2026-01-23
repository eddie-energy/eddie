// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity(name = "BeCreatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class CreatedEvent extends PersistablePermissionEvent {
    @Column(length = 36)
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    private final String connectionId;

    public CreatedEvent(String permissionId, String dataNeedId, String connectionId) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.dataNeedId = dataNeedId;
        this.connectionId = connectionId;
    }

    public CreatedEvent(String permissionId, String dataNeedId, String connectionId, ZonedDateTime created) {
        super(permissionId, PermissionProcessStatus.CREATED, created);
        this.dataNeedId = dataNeedId;
        this.connectionId = connectionId;
    }

    protected CreatedEvent() {
        this.dataNeedId = null;
        this.connectionId = null;
    }
}

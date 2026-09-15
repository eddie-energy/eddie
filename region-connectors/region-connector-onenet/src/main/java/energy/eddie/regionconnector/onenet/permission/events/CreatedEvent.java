// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.permission.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.onenet.permission.request.OneNetDataSourceInformation;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.time.ZonedDateTime;

@Entity(name = "OneNetCreatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class CreatedEvent extends PersistablePermissionEvent {
    private final String connectionId;
    private final String dataNeedId;
    @Transient
    private final OneNetDataSourceInformation dataSourceInformation = new OneNetDataSourceInformation();

    public CreatedEvent(String permissionId, String connectionId, String dataNeedId) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            ZonedDateTime created
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, created);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
    }

    protected CreatedEvent() {
        connectionId = null;
        dataNeedId = null;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }
}

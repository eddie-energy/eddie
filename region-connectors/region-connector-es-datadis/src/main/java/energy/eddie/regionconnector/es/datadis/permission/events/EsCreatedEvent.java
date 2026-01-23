// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.Clock;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsCreatedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    private final String nif;
    @Column(columnDefinition = "text")
    private final String meteringPointId;

    public EsCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String nif,
            String meteringPointId
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.nif = nif;
        this.meteringPointId = meteringPointId;
    }

    public EsCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String nif,
            String meteringPointId,
            Clock clock
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, clock);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.nif = nif;
        this.meteringPointId = meteringPointId;
    }

    protected EsCreatedEvent() {
        super();
        connectionId = null;
        dataNeedId = null;
        nif = null;
        meteringPointId = null;
    }
}

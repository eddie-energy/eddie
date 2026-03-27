// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.jspecify.annotations.Nullable;

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
    @Column
    @Nullable
    private final String firstname;
    @Column
    @Nullable
    private final String surname;

    public EsCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String nif,
            String meteringPointId,
            @Nullable String firstname,
            @Nullable String surname
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.nif = nif;
        this.meteringPointId = meteringPointId;
        this.firstname = firstname;
        this.surname = surname;
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
        this.firstname = null;
        this.surname = null;
    }

    protected EsCreatedEvent() {
        super();
        connectionId = null;
        dataNeedId = null;
        nif = null;
        meteringPointId = null;
        firstname = null;
        surname = null;
    }

    public String firstname() {
        return firstname;
    }

    public String surname() {
        return surname;
    }
}

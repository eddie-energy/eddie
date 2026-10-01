// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity(name = "OneNetValidatedEvent")
@SuppressWarnings({"unused", "NullAway"})
public class ValidatedEvent extends PersistablePermissionEvent {
    @Column(name = "granularity", nullable = false)
    @Enumerated(EnumType.STRING)
    private final Granularity granularity;
    @Column(name = "data_start", nullable = false)
    private final LocalDate dataStart;
    @Column(name = "data_end", nullable = false)
    private final LocalDate dataEnd;

    public ValidatedEvent(String permissionId, Granularity granularity, LocalDate dataStart, LocalDate dataEnd) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.granularity = granularity;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
    }

    protected ValidatedEvent() {
        super();
        granularity = null;
        dataStart = null;
        dataEnd = null;
    }

    public Granularity granularity() {
        return granularity;
    }

    public LocalDate start() {
        return dataStart;
    }

    public LocalDate end() {
        return dataEnd;
    }
}

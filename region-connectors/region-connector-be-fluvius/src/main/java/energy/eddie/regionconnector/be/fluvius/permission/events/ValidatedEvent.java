// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity(name = "BeValidatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class ValidatedEvent extends PersistablePermissionEvent {
    @Column(name = "data_start")
    private final LocalDate start;
    @Column(name = "data_end")
    private final LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text", length = 3)
    private final Flow flow;

    public ValidatedEvent(
            String permissionId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            Flow flow
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.flow = flow;
    }

    protected ValidatedEvent() {
        this.start = null;
        this.end = null;
        this.granularity = null;
        this.flow = null;
    }

    public Flow flow() {
        return flow;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public Granularity granularity() {
        return granularity;
    }
}

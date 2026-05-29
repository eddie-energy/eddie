// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDate;

/**
 * Validated permission event for accounting-point data needs.
 * Carries the permission timeframe but no granularity, since accounting-point
 * data is non-temporal master data.
 */
@Entity(name = "DeAccountingPointValidatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class AccountingPointValidatedEvent extends PersistablePermissionEvent {

    @Column(name = "data_start")
    private final LocalDate start;

    @Column(name = "data_end")
    private final LocalDate end;

    public AccountingPointValidatedEvent(String permissionId, LocalDate start, LocalDate end) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
    }

    protected AccountingPointValidatedEvent() {
        super();
        this.start = null;
        this.end = null;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }
}
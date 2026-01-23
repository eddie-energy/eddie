// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity(name = "DataReceivedEvent")
public class DataReceivedEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @Column(name = "meter_reading_start")
    private final LocalDate meterReadingStart;
    @Column(name = "meter_reading_end")
    private final LocalDate meterReadingEnd;

    public DataReceivedEvent(
            String permissionId,
            PermissionProcessStatus status,
            LocalDate meterReadingStart,
            LocalDate meterReadingEnd
    ) {
        super(permissionId, status);
        this.meterReadingStart = meterReadingStart;
        this.meterReadingEnd = meterReadingEnd;
    }

    @SuppressWarnings("NullAway")
    protected DataReceivedEvent() {
        meterReadingStart = null;
        meterReadingEnd = null;
    }

    public LocalDate start() {
        return meterReadingStart;
    }

    public LocalDate end() {
        return meterReadingEnd;
    }
}

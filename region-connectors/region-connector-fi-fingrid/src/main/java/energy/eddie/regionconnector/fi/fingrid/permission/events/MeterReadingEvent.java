// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.Map;

@SuppressWarnings({"NullAway", "unused"})
@Entity(name = "FiMeterReadingEvent")
public class MeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @ElementCollection
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "fi_fingrid")
    private final Map<String, ZonedDateTime> lastMeterReadings;

    public MeterReadingEvent(String permissionId, Map<String, ZonedDateTime> lastMeterReadings) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.lastMeterReadings = lastMeterReadings;
    }

    protected MeterReadingEvent() {
        lastMeterReadings = null;
    }

    public Map<String, ZonedDateTime> lastMeterReadings() {
        return lastMeterReadings;
    }
}
